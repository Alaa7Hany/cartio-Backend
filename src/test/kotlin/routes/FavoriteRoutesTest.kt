package com.example.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.database.FavoriteFacade
import com.example.models.ProductResponse
import com.example.utils.JwtConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
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
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteRoutesTest {

    private val mockFacade: FavoriteFacade = mockk()

    private val userId = "550e8400-e29b-41d4-a716-446655440000"
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

    private val sampleProduct = ProductResponse(
        id = productId,
        title = "Test Product",
        description = "Test Description",
        price = 9.99,
        imageUrl = "https://example.com/image.png",
        categoryId = "cat-1",
        isFeatured = false
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
            install(StatusPages) {
                exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
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
            routing { favoriteRoutes(mockFacade) }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
            }
            block(client)
        }

    // --- GET /favorites ---

    @Test
    fun `GET favorites returns 200 and list of products for authenticated user`() = setupApp { client ->
        coEvery { mockFacade.getUserFavorites(userId) } returns listOf(sampleProduct)

        val response = client.get("/favorites") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Favorites fetched successfully.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonArray
        assertEquals(1, data.size)
        assertEquals(productId, data[0].jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET favorites returns 401 when no token is provided`() = setupApp { client ->
        val response = client.get("/favorites")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET favorites returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.getUserFavorites(userId) } throws RuntimeException("DB error")

        val response = client.get("/favorites") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }

    // --- POST /favorites/{productId} ---

    @Test
    fun `POST favorites returns 200 on success`() = setupApp { client ->
        coEvery { mockFacade.addFavorite(userId, productId) } returns true

        val response = client.post("/favorites/$productId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Product added to favorites.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST favorites returns 400 when facade returns false`() = setupApp { client ->
        coEvery { mockFacade.addFavorite(userId, productId) } returns false

        val response = client.post("/favorites/$productId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `POST favorites returns 401 when no token is provided`() = setupApp { client ->
        val response = client.post("/favorites/$productId")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST favorites returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.addFavorite(userId, productId) } throws RuntimeException("DB error")

        val response = client.post("/favorites/$productId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }

    // --- DELETE /favorites/{productId} ---

    @Test
    fun `DELETE favorites returns 200 on success`() = setupApp { client ->
        coEvery { mockFacade.removeFavorite(userId, productId) } returns true

        val response = client.delete("/favorites/$productId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Product removed from favorites.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `DELETE favorites returns 404 when facade returns false`() = setupApp { client ->
        coEvery { mockFacade.removeFavorite(userId, productId) } returns false

        val response = client.delete("/favorites/$productId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `DELETE favorites returns 401 when no token is provided`() = setupApp { client ->
        val response = client.delete("/favorites/$productId")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE favorites returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.removeFavorite(userId, productId) } throws RuntimeException("DB error")

        val response = client.delete("/favorites/$productId") {
            header("Authorization", "Bearer $validToken")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }
}


