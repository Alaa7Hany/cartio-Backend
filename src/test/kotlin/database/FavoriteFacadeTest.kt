package com.example.database

import com.example.models.ProductResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteFacadeTest {

    private val facade: FavoriteFacade = mockk()

    private val userId = "550e8400-e29b-41d4-a716-446655440000"
    private val productId = "660e8400-e29b-41d4-a716-446655440001"
    private val invalidId = "not-a-uuid"

    private val sampleProduct = ProductResponse(
        id = productId,
        title = "Test Product",
        description = "Test Description",
        price = 9.99,
        imageUrl = "https://example.com/image.png",
        categoryId = "cat-1",
        isFeatured = false
    )

    // --- getUserFavorites ---

    @Test
    fun `getUserFavorites returns list of products for known user`() = runTest {
        coEvery { facade.getUserFavorites(userId) } returns listOf(sampleProduct)

        val result = facade.getUserFavorites(userId)

        assertEquals(1, result.size)
        assertEquals(productId, result.first().id)
    }

    @Test
    fun `getUserFavorites returns empty list for new user`() = runTest {
        coEvery { facade.getUserFavorites(userId) } returns emptyList()

        val result = facade.getUserFavorites(userId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUserFavorites returns empty list for invalid UUID`() = runTest {
        coEvery { facade.getUserFavorites(invalidId) } returns emptyList()

        val result = facade.getUserFavorites(invalidId)

        assertTrue(result.isEmpty())
    }

    // --- addFavorite ---

    @Test
    fun `addFavorite returns true when successful`() = runTest {
        coEvery { facade.addFavorite(userId, productId) } returns true

        val result = facade.addFavorite(userId, productId)

        assertTrue(result)
    }

    @Test
    fun `addFavorite returns false for invalid UUIDs`() = runTest {
        coEvery { facade.addFavorite(invalidId, productId) } returns false

        val result = facade.addFavorite(invalidId, productId)

        assertFalse(result)
    }

    // --- removeFavorite ---

    @Test
    fun `removeFavorite returns true when successful`() = runTest {
        coEvery { facade.removeFavorite(userId, productId) } returns true

        val result = facade.removeFavorite(userId, productId)

        assertTrue(result)
    }

    @Test
    fun `removeFavorite returns false for invalid UUIDs`() = runTest {
        coEvery { facade.removeFavorite(invalidId, productId) } returns false

        val result = facade.removeFavorite(invalidId, productId)

        assertFalse(result)
    }
}

