package com.example.routes

import com.example.database.UserFacade
import com.example.models.AuthUser
import com.example.models.UserResponse
import com.example.utils.JwtConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Date
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthRoutesTest {

    private lateinit var mockFacade: UserFacade

    private val jwtSecret = "test-secret"
    private val validEmail = "john@example.com"
    private val validToken: String by lazy {
        JWT.create()
            .withIssuer("cartio-backend")
            .withAudience("cartio-users")
            .withClaim("email", validEmail)
            .withClaim("userId", "some-uuid")
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    private val noEmailToken: String by lazy {
        JWT.create()
            .withIssuer("cartio-backend")
            .withAudience("cartio-users")
            .withClaim("userId", "some-uuid")
            .withExpiresAt(Date(System.currentTimeMillis() + 86_400_000L))
            .sign(Algorithm.HMAC256(jwtSecret))
    }

    @BeforeTest
    fun setup() {
        mockFacade = mockk()
        mockkObject(JwtConfig)
        every { JwtConfig.generateToken(any(), any()) } returns "mocked-jwt-token"
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(JwtConfig)
    }

    private val validPayload = """
        {
            "email": "john@example.com",
            "password": "supersecret123",
            "fullName": "John Doe"
        }
    """.trimIndent()

    private val loginPayload = """
        {
            "email": "john@example.com",
            "password": "supersecret123"
        }
    """.trimIndent()

    private val fakeUserResponse = UserResponse(
        id = "some-uuid",
        email = "john@example.com",
        fullName = "John Doe",
        createdAt = "2026-01-01T00:00:00Z"
    )

    private val fakeAuthUser = AuthUser(
        id = "some-uuid",
        email = "john@example.com",
        passwordHash = com.example.utils.SecurityUtils.hashPassword("supersecret123"),
        fullName = "John Doe",
        createdAt = "2026-01-01T00:00:00Z"
    )

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
                        if (credential.payload.getClaim("email").asString() != null || credential.payload.getClaim("userId").asString() != null) {
                            JWTPrincipal(credential.payload)
                        } else null
                    }
                }
            }
            routing { authRoutes(mockFacade) }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
            }
            block(client)
        }

    // --- Register Tests ---

    @Test
    fun `POST register returns 201 and success true on valid request`() = setupApp { client ->
        coEvery { mockFacade.insertUser(any(), any()) } returns fakeUserResponse

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(validPayload)
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("john@example.com", body["data"]!!.jsonObject["email"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST register returns 400 and success false when facade returns null`() = setupApp { client ->
        coEvery { mockFacade.insertUser(any(), any()) } returns null

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(validPayload)
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `POST register returns 400 when facade throws an exception`() = setupApp { client ->
        coEvery { mockFacade.insertUser(any(), any()) } throws RuntimeException("DB connection failed")

        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody(validPayload)
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("An unexpected error occurred during registration.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST register returns 400 on malformed JSON body`() = setupApp { client ->
        val response = client.post("/register") {
            contentType(ContentType.Application.Json)
            setBody("{ bad json }")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // --- Login Tests ---

    @Test
    fun `POST login returns 200 and user data on valid credentials`() = setupApp { client ->
        coEvery { mockFacade.getUserByEmail("john@example.com") } returns fakeAuthUser

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(loginPayload)
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Login successful.", body["message"]!!.jsonPrimitive.content)
        assertEquals("john@example.com", body["data"]!!.jsonObject["email"]!!.jsonPrimitive.content)
        assertEquals("John Doe", body["data"]!!.jsonObject["fullName"]!!.jsonPrimitive.content)
        assertEquals("mocked-jwt-token", body["data"]!!.jsonObject["token"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST login returns 401 when user does not exist`() = setupApp { client ->
        coEvery { mockFacade.getUserByEmail(any()) } returns null

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(loginPayload)
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Invalid email or password.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST login returns 401 when password does not match`() = setupApp { client ->
        coEvery { mockFacade.getUserByEmail("john@example.com") } returns fakeAuthUser.copy(
            passwordHash = com.example.utils.SecurityUtils.hashPassword("different-password")
        )

        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody(loginPayload)
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Invalid email or password.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST login returns 400 on malformed JSON body`() = setupApp { client ->
        val response = client.post("/login") {
            contentType(ContentType.Application.Json)
            setBody("{ bad json }")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // --- Auth Me (Session Restoration) Tests ---

    @Test
    fun `GET auth me returns 200 and user data when token is valid`() = setupApp { client ->
        coEvery { mockFacade.getUserByEmail(validEmail) } returns fakeAuthUser

        val response = client.get("/auth/me") {
            header("Authorization", "Bearer $validToken")
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Session restored successfully.", body["message"]!!.jsonPrimitive.content)
        assertEquals(validEmail, body["data"]!!.jsonObject["email"]!!.jsonPrimitive.content)
        assertEquals("John Doe", body["data"]!!.jsonObject["fullName"]!!.jsonPrimitive.content)
        assertEquals("mocked-jwt-token", body["data"]!!.jsonObject["token"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET auth me returns 404 when user does not exist in db`() = setupApp { client ->
        coEvery { mockFacade.getUserByEmail(validEmail) } returns null

        val response = client.get("/auth/me") {
            header("Authorization", "Bearer $validToken")
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("User not found.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET auth me returns 401 when token has missing or invalid email`() = setupApp { client ->
        val response = client.get("/auth/me") {
            header("Authorization", "Bearer $noEmailToken")
        }

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Unauthorized.", body["message"]!!.jsonPrimitive.content)
    }
}