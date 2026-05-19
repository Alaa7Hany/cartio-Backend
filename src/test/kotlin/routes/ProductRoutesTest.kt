package com.example.routes

import com.example.database.ProductFacade
import com.example.models.ProductResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
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
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductRoutesTest {

    private val mockFacade: ProductFacade = mockk()

    private val sampleProduct = ProductResponse(
        id = "550e8400-e29b-41d4-a716-446655440000",
        title = "Test Product",
        description = "A great product",
        price = 9.99,
        imageUrl = "https://example.com/image.png",
        categoryId = "cat-01"
    )

    private val singleProductJson = """
        {
            "title": "Test Product",
            "description": "A great product",
            "price": 9.99,
            "imageUrl": "https://example.com/image.png",
            "categoryId": "cat-01"
        }
    """.trimIndent()

    private val batchProductsJson = """
        [
            {
                "title": "Product One",
                "description": "First product",
                "price": 4.99,
                "imageUrl": "https://example.com/one.png",
                "categoryId": "cat-01"
            },
            {
                "title": "Product Two",
                "description": "Second product",
                "price": 14.99,
                "imageUrl": "https://example.com/two.png",
                "categoryId": "cat-02"
            }
        ]
    """.trimIndent()

    private fun setupApp(block: suspend ApplicationTestBuilder.(HttpClient) -> Unit) =
        testApplication {
            install(ContentNegotiation) { json() }
            install(io.ktor.server.plugins.statuspages.StatusPages) {
                exception<io.ktor.server.plugins.BadRequestException> { call, _ ->
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            routing { productRoutes(mockFacade) }
            val client = createClient {
                install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) { json() }
            }
            block(client)
        }

    // --- GET /products ---

    @Test
    fun `GET products returns 200 and product list`() = setupApp { client ->
        coEvery { mockFacade.getAllProducts(1, 20) } returns listOf(sampleProduct)

        val response = client.get("/products")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Products fetched successfully.", body["message"]!!.jsonPrimitive.content)
        assertEquals(1, body["data"]!!.jsonArray.size)
        assertEquals(sampleProduct.id, body["data"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET products with pagination parameters returns 200 and product list`() = setupApp { client ->
        coEvery { mockFacade.getAllProducts(2, 5) } returns listOf(sampleProduct)

        val response = client.get("/products?page=2&limit=5")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Products fetched successfully.", body["message"]!!.jsonPrimitive.content)
        assertEquals(1, body["data"]!!.jsonArray.size)
        assertEquals(sampleProduct.id, body["data"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET products returns 200 with empty list when no products exist`() = setupApp { client ->
        coEvery { mockFacade.getAllProducts(1, 20) } returns emptyList()

        val response = client.get("/products")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(0, body["data"]!!.jsonArray.size)
    }

    @Test
    fun `GET products returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.getAllProducts(any(), any()) } throws RuntimeException("DB error")

        val response = client.get("/products")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while fetching products.",
            body["message"]!!.jsonPrimitive.content
        )
        assertNull(body["data"]?.jsonPrimitive?.contentOrNull)
    }

    // --- GET /products/{id} ---

    @Test
    fun `GET products by id returns 200 and product when found`() = setupApp { client ->
        coEvery { mockFacade.getProductById(sampleProduct.id) } returns sampleProduct

        val response = client.get("/products/${sampleProduct.id}")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Product fetched successfully.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonObject
        assertEquals(sampleProduct.id, data["id"]!!.jsonPrimitive.content)
        assertEquals(sampleProduct.title, data["title"]!!.jsonPrimitive.content)
        assertEquals(sampleProduct.price.toString(), data["price"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET products by id returns 404 when product not found`() = setupApp { client ->
        val unknownId = "00000000-0000-0000-0000-000000000000"
        coEvery { mockFacade.getProductById(unknownId) } returns null

        val response = client.get("/products/$unknownId")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("No product found with ID: $unknownId", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET products by id returns 404 for malformed UUID`() = setupApp { client ->
        val malformedId = "not-a-valid-uuid"
        coEvery { mockFacade.getProductById(malformedId) } returns null

        val response = client.get("/products/$malformedId")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun `GET products by id returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.getProductById(any()) } throws RuntimeException("DB error")

        val response = client.get("/products/${sampleProduct.id}")
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while fetching the product.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    // --- POST /products ---

    @Test
    fun `POST products returns 201 and created product`() = setupApp { client ->
        coEvery { mockFacade.insertProduct(any()) } returns sampleProduct

        val response = client.post("/products") {
            contentType(ContentType.Application.Json)
            setBody(singleProductJson)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Product created successfully.", body["message"]!!.jsonPrimitive.content)

        val data = body["data"]!!.jsonObject
        assertNotNull(data["id"])
        assertEquals(sampleProduct.title, data["title"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST products returns 500 when facade returns null`() = setupApp { client ->
        coEvery { mockFacade.insertProduct(any()) } returns null

        val response = client.post("/products") {
            contentType(ContentType.Application.Json)
            setBody(singleProductJson)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Product could not be created.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST products returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.insertProduct(any()) } throws RuntimeException("DB error")

        val response = client.post("/products") {
            contentType(ContentType.Application.Json)
            setBody(singleProductJson)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred while creating the product.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `POST products returns 400 on malformed JSON`() = setupApp { client ->
        val response = client.post("/products") {
            contentType(ContentType.Application.Json)
            setBody("{ bad json }")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    // --- POST /products/batch ---

    @Test
    fun `POST products batch returns 201 and success message`() = setupApp { client ->
        coEvery { mockFacade.insertProducts(any()) } returns true

        val response = client.post("/products/batch") {
            contentType(ContentType.Application.Json)
            setBody(batchProductsJson)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Batch insert successful.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST products batch returns 400 on empty list`() = setupApp { client ->
        val response = client.post("/products/batch") {
            contentType(ContentType.Application.Json)
            setBody("[]")
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "Request body must contain at least one product.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `POST products batch returns 500 when facade returns false`() = setupApp { client ->
        coEvery { mockFacade.insertProducts(any()) } returns false

        val response = client.post("/products/batch") {
            contentType(ContentType.Application.Json)
            setBody(batchProductsJson)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals("Batch insert failed. No products were saved.", body["message"]!!.jsonPrimitive.content)
    }

    @Test
    fun `POST products batch returns 500 when facade throws`() = setupApp { client ->
        coEvery { mockFacade.insertProducts(any()) } throws RuntimeException("DB error")

        val response = client.post("/products/batch") {
            contentType(ContentType.Application.Json)
            setBody(batchProductsJson)
        }
        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertEquals(HttpStatusCode.InternalServerError, response.status)
        assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
        assertEquals(
            "An unexpected error occurred during batch insert.",
            body["message"]!!.jsonPrimitive.content
        )
    }

    @Test
    fun `POST products batch returns 400 on malformed JSON`() = setupApp { client ->
        val response = client.post("/products/batch") {
            contentType(ContentType.Application.Json)
            setBody("{ bad json }")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

        // --- GET /products/categories ---

        @Test
        fun `GET products categories returns 200 and category list`() = setupApp { client ->
            coEvery { mockFacade.getAvailableCategories() } returns listOf("cat-01", "cat-02")

            val response = client.get("/products/categories")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("Categories fetched successfully.", body["message"]!!.jsonPrimitive.content)
            assertEquals(2, body["data"]!!.jsonArray.size)
            assertEquals("cat-01", body["data"]!!.jsonArray[0].jsonPrimitive.content)
            assertEquals("cat-02", body["data"]!!.jsonArray[1].jsonPrimitive.content)
        }

        @Test
        fun `GET products categories returns 200 with empty list when no products exist`() = setupApp { client ->
            coEvery { mockFacade.getAvailableCategories() } returns emptyList()

            val response = client.get("/products/categories")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals(0, body["data"]!!.jsonArray.size)
        }

        @Test
        fun `GET products categories returns 500 when facade throws`() = setupApp { client ->
            coEvery { mockFacade.getAvailableCategories() } throws RuntimeException("DB error")

            val response = client.get("/products/categories")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals(
                "An unexpected error occurred while fetching categories.",
                body["message"]!!.jsonPrimitive.content
            )
        }

        // --- GET /products/search ---

        @Test
        fun `GET products search returns 200 and list of matching products`() = setupApp { client ->
            coEvery { mockFacade.searchProducts("test") } returns listOf(sampleProduct)

            val response = client.get("/products/search?q=test")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("Search completed successfully.", body["message"]!!.jsonPrimitive.content)
            assertEquals(1, body["data"]!!.jsonArray.size)
            assertEquals(sampleProduct.id, body["data"]!!.jsonArray[0].jsonObject["id"]!!.jsonPrimitive.content)
        }

        @Test
        fun `GET products search returns 400 when query is blank`() = setupApp { client ->
            val response = client.get("/products/search?q=")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("Search query must not be empty.", body["message"]!!.jsonPrimitive.content)
        }

        @Test
        fun `GET products search returns 400 when query is missing`() = setupApp { client ->
            val response = client.get("/products/search")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("Search query must not be empty.", body["message"]!!.jsonPrimitive.content)
        }

        @Test
        fun `GET products search returns 500 when facade throws`() = setupApp { client ->
            coEvery { mockFacade.searchProducts(any()) } throws RuntimeException("DB error")

            val response = client.get("/products/search?q=test")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals(
                "An unexpected error occurred during search.",
                body["message"]!!.jsonPrimitive.content
            )
        }

        // --- GET /products/category/{categoryId} ---

        @Test
        fun `GET products by category returns 200 and matching products`() = setupApp { client ->
            coEvery { mockFacade.getProductsByCategory("cat-01") } returns listOf(sampleProduct)

            val response = client.get("/products/category/cat-01")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("Products fetched successfully.", body["message"]!!.jsonPrimitive.content)
            assertEquals(1, body["data"]!!.jsonArray.size)
            assertEquals(
                sampleProduct.categoryId,
                body["data"]!!.jsonArray[0].jsonObject["categoryId"]!!.jsonPrimitive.content
            )
        }

        @Test
        fun `GET products by category returns 200 with empty list and appropriate message`() = setupApp { client ->
            coEvery { mockFacade.getProductsByCategory("empty-cat") } returns emptyList()

            val response = client.get("/products/category/empty-cat")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.OK, response.status)
            assertTrue(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals("No products found in category: empty-cat", body["message"]!!.jsonPrimitive.content)
            assertEquals(0, body["data"]!!.jsonArray.size)
        }

        @Test
        fun `GET products by category returns 500 when facade throws`() = setupApp { client ->
            coEvery { mockFacade.getProductsByCategory(any()) } throws RuntimeException("DB error")

            val response = client.get("/products/category/cat-01")
            val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertFalse(body["success"]!!.jsonPrimitive.content.toBoolean())
            assertEquals(
                "An unexpected error occurred while fetching products by category.",
                body["message"]!!.jsonPrimitive.content
            )
        }
    }
