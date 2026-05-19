package com.example.routes

import com.example.database.CartFacade
import com.example.models.CartItemResponse
import com.example.models.CartResponse
import com.example.utils.JwtConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CartRoutesTest {

    private val mockFacade: CartFacade = mockk()

    private val userId = "550e8400-e29b-41d4-a716-446655440000"
    private val itemId = "770e8400-e29b-41d4-a716-446655440002"
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

    private val sampleItem = CartItemResponse(
        id = itemId,
        productId = productId,
        title = "Test Product",
        price = 9.99,
        imageUrl = "https://example.com/image.png",
        quantity = 2
    )

    private val sampleCart = CartResponse(
        id = "880e8400-e29b-41d4-a716-446655440003",
        userId = userId,
        items = listOf(sampleItem),
        subtotal = 19.98
    )

    private val emptyCart = CartResponse(
        id = "880e8400-e29b-41d4-a716-446655440003",
        userId = userId,
        items = emptyList(),
        subtotal = 0.0
    )

    @BeforeTest
    fun setup() {
        mockkObject(JwtConfig)
        every { JwtConfig.generateToken(any(), any()) } returns validToken
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(JwtConfig)
    }

    private fun setupApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            install(ContentNegotiation) { json() }
            install(io.ktor.server.plugins.statuspages.StatusPages) {
                exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            install(io.ktor.server.auth.Authentication) {
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
            routing { cartRoutes(mockFacade) }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
            }
            block(client)
        }

    // --- GET /cart ---

    @Test
    fun `GET cart returns 200 and cart data for authenticated user`() = setupApp { client ->
        coEvery { mockFacade.getCartForUser(userId) } returns sampleCart

        val response = client.get("/cart") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Cart fetched successfully.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonObject
        assertEquals(userId, data["userId"]!!.jsonPrimitive.content)
        assertEquals(1, data["items"]!!.jsonArray.size)
        assertEquals("19.98", data["subtotal"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET cart returns 200 with empty items for new user`() = setupApp { client ->
        coEvery { mockFacade.getCartForUser(userId) } returns emptyCart

        val response = client.get("/cart") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(0, body["data"]!!.jsonObject["items"]!!.jsonArray.size)
    }

    @Test
    fun `GET cart returns 401 when no token is provided`() = setupApp { client ->
        val response = client.get("/cart")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET cart returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.getCartForUser(userId) } throws RuntimeException("DB error")

        val response = client.get("/cart") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while fetching the cart.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    // --- POST /cart/add ---

    @Test
    fun `POST cart add returns 200 and updated cart`() = setupApp { client ->
        coEvery { mockFacade.addToCart(userId, any()) } returns sampleCart

        val response = client.post("/cart/add") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("""{"productId":"$productId","quantity":2}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Item added to cart successfully.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonObject
        assertEquals(1, data["items"]!!.jsonArray.size)
        assertEquals(productId, data["items"]!!.jsonArray[0].jsonObject["productId"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST cart add returns 401 when no token is provided`() = setupApp { client ->
        val response = client.post("/cart/add") {
            contentType(ContentType.Application.Json)
            setBody("""{"productId":"$productId","quantity":1}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST cart add returns 400 on malformed JSON`() = setupApp { client ->
        val response = client.post("/cart/add") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("{ bad json }")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Invalid request body.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST cart add returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.addToCart(userId, any()) } throws RuntimeException("DB error")

        val response = client.post("/cart/add") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("""{"productId":"$productId","quantity":1}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while adding item to cart.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    // --- PUT /cart/{itemId} ---

    @Test
    fun `PUT cart itemId returns 200 and updated cart`() = setupApp { client ->
        val updatedItem = sampleItem.copy(quantity = 5)
        val updatedCart = sampleCart.copy(items = listOf(updatedItem), subtotal = 49.95)
        coEvery { mockFacade.updateCartItem(userId, itemId, 5) } returns updatedCart

        val response = client.put("/cart/$itemId") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":5}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Cart item updated successfully.", body["message"]!!.jsonPrimitive.content)
        assertEquals(5, body["data"]!!.jsonObject["items"]!!.jsonArray[0].jsonObject["quantity"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun `PUT cart itemId returns 404 when facade returns null`() = setupApp { client ->
        coEvery { mockFacade.updateCartItem(userId, itemId, 3) } returns null

        val response = client.put("/cart/$itemId") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":3}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Cart item not found.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `PUT cart itemId returns 401 when no token is provided`() = setupApp { client ->
        val response = client.put("/cart/$itemId") {
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":1}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT cart itemId returns 400 on malformed JSON`() = setupApp { client ->
        val response = client.put("/cart/$itemId") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("{ bad json }")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Invalid request body.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `PUT cart itemId returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.updateCartItem(userId, itemId, 2) } throws RuntimeException("DB error")

        val response = client.put("/cart/$itemId") {
            header("Authorization", "Bearer $validToken")
            contentType(ContentType.Application.Json)
            setBody("""{"quantity":2}""")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while updating the cart item.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    // --- DELETE /cart/{itemId} ---

    @Test
    fun `DELETE cart itemId returns 200 and cart without removed item`() = setupApp { client ->
        coEvery { mockFacade.removeCartItem(userId, itemId) } returns emptyCart

        val response = client.delete("/cart/$itemId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Cart item removed successfully.", body["message"]!!.jsonPrimitive.content)
        assertEquals(0, body["data"]!!.jsonObject["items"]!!.jsonArray.size)
    }

    @Test
    fun `DELETE cart itemId returns 401 when no token is provided`() = setupApp { client ->
        val response = client.delete("/cart/$itemId")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE cart itemId returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.removeCartItem(userId, itemId) } throws RuntimeException("DB error")

        val response = client.delete("/cart/$itemId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while removing the cart item.",
            body["message"]!!.jsonPrimitive.content
        )
    }
}
