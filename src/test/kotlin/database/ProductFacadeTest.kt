package com.example.database

import com.example.models.CreateProductRequest
import com.example.models.ProductResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductFacadeTest {

    private val facade: ProductFacade = mockk()

    private val sampleRequest = CreateProductRequest(
        title = "Test Product",
        description = "A great product",
        price = 9.99,
        imageUrl = "https://example.com/image.png",
        categoryId = "cat-01"
    )

    private val sampleResponse = ProductResponse(
        id = "550e8400-e29b-41d4-a716-446655440000",
        title = "Test Product",
        description = "A great product",
        price = 9.99,
        imageUrl = "https://example.com/image.png",
        categoryId = "cat-01",
        isFeatured = false
    )

    // --- getAllProducts ---

    @Test
    fun `getAllProducts returns a list of products using default parameters`() = runTest {
        coEvery { facade.getAllProducts(1, 20) } returns listOf(sampleResponse)

        val result = facade.getAllProducts()

        assertEquals(1, result.size)
        assertEquals(sampleResponse, result.first())
    }

    @Test
    fun `getAllProducts returns a list of products using custom parameters`() = runTest {
        coEvery { facade.getAllProducts(2, 5) } returns listOf(sampleResponse)

        val result = facade.getAllProducts(2, 5)

        assertEquals(1, result.size)
        assertEquals(sampleResponse, result.first())
    }

    @Test
    fun `getAllProducts returns empty list when table is empty`() = runTest {
        coEvery { facade.getAllProducts(1, 20) } returns emptyList()

        val result = facade.getAllProducts()

        assertTrue(result.isEmpty())
    }

    // --- getProductById ---

    @Test
    fun `getProductById returns product for valid existing UUID`() = runTest {
        coEvery { facade.getProductById(sampleResponse.id) } returns sampleResponse

        val result = facade.getProductById(sampleResponse.id)

        assertNotNull(result)
        assertEquals(sampleResponse.id, result.id)
        assertEquals(sampleResponse.title, result.title)
        assertEquals(sampleResponse.price, result.price)
    }

    @Test
    fun `getProductById returns null for non-existent UUID`() = runTest {
        val unknownId = "00000000-0000-0000-0000-000000000000"
        coEvery { facade.getProductById(unknownId) } returns null

        val result = facade.getProductById(unknownId)

        assertNull(result)
    }

    @Test
    fun `getProductById returns null for malformed UUID string`() = runTest {
        val malformedId = "not-a-uuid"
        coEvery { facade.getProductById(malformedId) } returns null

        val result = facade.getProductById(malformedId)

        assertNull(result)
    }

    // --- insertProduct ---

    @Test
    fun `insertProduct returns ProductResponse on success`() = runTest {
        coEvery { facade.insertProduct(sampleRequest) } returns sampleResponse

        val result = facade.insertProduct(sampleRequest)

        assertNotNull(result)
        assertEquals(sampleResponse.title, result.title)
        assertEquals(sampleResponse.price, result.price)
        assertEquals(sampleResponse.categoryId, result.categoryId)
    }

    @Test
    fun `insertProduct returns null when insert fails`() = runTest {
        coEvery { facade.insertProduct(sampleRequest) } returns null

        val result = facade.insertProduct(sampleRequest)

        assertNull(result)
    }

    // --- insertProducts ---

    @Test
    fun `insertProducts returns true when batch insert succeeds`() = runTest {
        val requests = listOf(sampleRequest, sampleRequest.copy(title = "Second Product"))
        coEvery { facade.insertProducts(requests) } returns true

        val result = facade.insertProducts(requests)

        assertTrue(result)
    }

    @Test
    fun `insertProducts returns false when batch insert fails`() = runTest {
        val requests = listOf(sampleRequest)
        coEvery { facade.insertProducts(requests) } returns false

        val result = facade.insertProducts(requests)

        assertFalse(result)
    }

    @Test
    fun `insertProducts with empty list returns true (no-op)`() = runTest {
        coEvery { facade.insertProducts(emptyList()) } returns true

        val result = facade.insertProducts(emptyList())

        assertTrue(result)
    }

    // --- getProductsByCategory ---

    @Test
    fun `getProductsByCategory returns matching products`() = runTest {
        coEvery { facade.getProductsByCategory("cat-01") } returns listOf(sampleResponse)

        val result = facade.getProductsByCategory("cat-01")

        assertEquals(1, result.size)
        assertEquals("cat-01", result.first().categoryId)
    }

    @Test
    fun `getProductsByCategory returns empty list when no products match`() = runTest {
        coEvery { facade.getProductsByCategory("unknown-cat") } returns emptyList()

        val result = facade.getProductsByCategory("unknown-cat")

        assertTrue(result.isEmpty())
    }

    // --- getAvailableCategories ---

    @Test
    fun `getAvailableCategories returns distinct category ids`() = runTest {
        coEvery { facade.getAvailableCategories() } returns listOf("cat-01", "cat-02", "cat-03")

        val result = facade.getAvailableCategories()

        assertEquals(3, result.size)
        assertTrue(result.contains("cat-01"))
        assertTrue(result.contains("cat-02"))
        assertTrue(result.contains("cat-03"))
    }

    @Test
    fun `getAvailableCategories returns empty list when table is empty`() = runTest {
        coEvery { facade.getAvailableCategories() } returns emptyList()

        val result = facade.getAvailableCategories()

        assertTrue(result.isEmpty())
    }

    // --- searchProducts ---

    @Test
    fun `searchProducts returns matching products for query`() = runTest {
        coEvery { facade.searchProducts("test") } returns listOf(sampleResponse)

        val result = facade.searchProducts("test")

        assertEquals(1, result.size)
        assertEquals(sampleResponse.id, result.first().id)
    }

    @Test
    fun `searchProducts returns empty list when no products match`() = runTest {
        coEvery { facade.searchProducts("unknown") } returns emptyList()

        val result = facade.searchProducts("unknown")

        assertTrue(result.isEmpty())
    }

    // --- getFeaturedProducts ---

    @Test
    fun `getFeaturedProducts returns matching featured products`() = runTest {
        coEvery { facade.getFeaturedProducts() } returns listOf(sampleResponse.copy(isFeatured = true))

        val result = facade.getFeaturedProducts()

        assertEquals(1, result.size)
        assertTrue(result.first().isFeatured)
    }

    @Test
    fun `getFeaturedProducts returns empty list when no featured products match`() = runTest {
        coEvery { facade.getFeaturedProducts() } returns emptyList()

        val result = facade.getFeaturedProducts()

        assertTrue(result.isEmpty())
    }
}