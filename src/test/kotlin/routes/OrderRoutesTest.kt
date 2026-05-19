package com.example.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.database.OrderFacade
import com.example.models.OrderItemResponse
import com.example.models.OrderResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderRoutesTest {

    private val mockFacade: OrderFacade = mockk()

    private val userId = "550e8400-e29b-41d4-a716-446655440000"
    private val orderId = "aa0e8400-e29b-41d4-a716-446655440010"
    private val orderItemId = "bb0e8400-e29b-41d4-a716-446655440011"
    private val productId = "660e8400-e29b-41d4-a716-446655440001"

    private val jwtSecret = "test-secret"
    private val validToken: String by lazy {
        JWT.create()
            .withIssuer("cartio-backend")
            .withAudience("cartio-users")
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private val sampleOrderItem = OrderItemResponse(
        id = orderItemId,
        productId = productId,
        title = "Test Product",
        imageUrl = "https://example.com/image.png",
        quantity = 2,
        priceAtCheckout = 9.99
    )

    private val sampleOrder = OrderResponse(
        id = orderId,
        userId = userId,
        items = listOf(sampleOrderItem),
        totalAmount = 19.98,
        orderDate = "2026-05-19T10:00:00Z"
    )

    private fun setupApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            install(ContentNegotiation) { json() }
            install(Authentication) {
                jwt("auth-jwt") {
                    realm = "cartio-backend"
                    verifier(
                        JWT.require(Algorithm.HMAC256(jwtSecret))
                            .withIssuer("cartio-backend")
                            .withAudience("cartio-users")
                            .build()
                    )
                    validate { credential ->
                        if (credential.payload.getClaim("userId").asString() != null) {
                            JWTPrincipal(credential.payload)
                        } else null
                    }
                }
            }
            routing { orderRoutes(mockFacade) }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
            }
            block(client)
        }

    // --- POST /checkout ---

    @Test
    fun `POST checkout returns 200 and order on successful checkout`() = setupApp { client ->
        coEvery { mockFacade.checkoutCart(userId) } returns sampleOrder

        val response = client.post("/checkout") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Checkout successful.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonObject
        assertEquals(orderId, data["id"]!!.jsonPrimitive.content)
        assertEquals(userId, data["userId"]!!.jsonPrimitive.content)
        assertEquals("19.98", data["totalAmount"]!!.jsonPrimitive.content)
        assertEquals(1, data["items"]!!.jsonArray.size)
    }

    @Test
    fun `POST checkout returns 200 with correct item details`() = setupApp { client ->
        coEvery { mockFacade.checkoutCart(userId) } returns sampleOrder

        val response = client.post("/checkout") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val item = body["data"]!!.jsonObject["items"]!!.jsonArray.first().jsonObject

        assertEquals(orderItemId, item["id"]!!.jsonPrimitive.content)
        assertEquals(productId, item["productId"]!!.jsonPrimitive.content)
        assertEquals("Test Product", item["title"]!!.jsonPrimitive.content)
        assertEquals("9.99", item["priceAtCheckout"]!!.jsonPrimitive.content)
        assertEquals("2", item["quantity"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST checkout returns 400 when cart is empty or not found`() = setupApp { client ->
        coEvery { mockFacade.checkoutCart(userId) } returns null

        val response = client.post("/checkout") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Cannot checkout: cart is empty or not found.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST checkout returns 401 when no token is provided`() = setupApp { client ->
        val response = client.post("/checkout")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST checkout returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.checkoutCart(userId) } throws RuntimeException("DB error")

        val response = client.post("/checkout") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("An unexpected error occurred during checkout.", body["message"]!!.jsonPrimitive.content)
    }

    // --- GET /orders ---

    @Test
    fun `GET orders returns 200 and list of orders`() = setupApp { client ->
        coEvery { mockFacade.getUserOrders(userId) } returns listOf(sampleOrder)

        val response = client.get("/orders") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Orders fetched successfully.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonArray
        assertEquals(1, data.size)
        assertEquals(orderId, data.first().jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET orders returns 200 with empty array and appropriate message when no orders`() = setupApp { client ->
        coEvery { mockFacade.getUserOrders(userId) } returns emptyList()

        val response = client.get("/orders") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("No past orders found.", body["message"]!!.jsonPrimitive.content)
        assertEquals(0, body["data"]!!.jsonArray.size)
    }

    @Test
    fun `GET orders returns 401 when no token is provided`() = setupApp { client ->
        val response = client.get("/orders")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET orders returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.getUserOrders(userId) } throws RuntimeException("DB error")

        val response = client.get("/orders") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while fetching orders.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `GET orders preserves priceAtCheckout per item`() = setupApp { client ->
        val historicalOrder = sampleOrder.copy(
            items = listOf(sampleOrderItem.copy(priceAtCheckout = 4.99))
        )
        coEvery { mockFacade.getUserOrders(userId) } returns listOf(historicalOrder)

        val response = client.get("/orders") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val item = body["data"]!!.jsonArray.first().jsonObject["items"]!!.jsonArray.first().jsonObject

        assertEquals("4.99", item["priceAtCheckout"]!!.jsonPrimitive.content)
    }
}
