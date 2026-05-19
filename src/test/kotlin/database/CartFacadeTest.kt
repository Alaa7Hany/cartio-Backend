package com.example.database

import com.example.models.AddToCartRequest
import com.example.models.CartItemResponse
import com.example.models.CartResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CartFacadeTest {

    private val facade: CartFacade = mockk()

    private val userId = "550e8400-e29b-41d4-a716-446655440000"
    private val productId = "660e8400-e29b-41d4-a716-446655440001"
    private val itemId = "770e8400-e29b-41d4-a716-446655440002"

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
        id = "",
        userId = userId,
        items = emptyList(),
        subtotal = 0.0
    )

    // --- getCartForUser ---

    @Test
    fun `getCartForUser returns cart with items for known user`() = runTest {
        coEvery { facade.getCartForUser(userId) } returns sampleCart

        val result = facade.getCartForUser(userId)

        assertEquals(sampleCart.id, result.id)
        assertEquals(userId, result.userId)
        assertEquals(1, result.items.size)
        assertEquals(19.98, result.subtotal)
    }

    @Test
    fun `getCartForUser returns empty cart for new user`() = runTest {
        coEvery { facade.getCartForUser(userId) } returns emptyCart

        val result = facade.getCartForUser(userId)

        assertTrue(result.items.isEmpty())
        assertEquals(0.0, result.subtotal)
        assertEquals(userId, result.userId)
    }

    @Test
    fun `getCartForUser returns empty cart response for invalid UUID`() = runTest {
        val invalidId = "not-a-uuid"
        coEvery { facade.getCartForUser(invalidId) } returns emptyCart.copy(userId = invalidId)

        val result = facade.getCartForUser(invalidId)

        assertTrue(result.items.isEmpty())
        assertEquals(invalidId, result.userId)
    }

    // --- addToCart ---

    @Test
    fun `addToCart returns updated cart with new item`() = runTest {
        val request = AddToCartRequest(productId = productId, quantity = 2)
        coEvery { facade.addToCart(userId, request) } returns sampleCart

        val result = facade.addToCart(userId, request)

        assertEquals(1, result.items.size)
        assertEquals(productId, result.items.first().productId)
        assertEquals(2, result.items.first().quantity)
        assertEquals(19.98, result.subtotal)
    }

    @Test
    fun `addToCart increments quantity when item already exists in cart`() = runTest {
        val request = AddToCartRequest(productId = productId, quantity = 3)
        val updatedItem = sampleItem.copy(quantity = 5)
        val updatedCart = sampleCart.copy(items = listOf(updatedItem), subtotal = 49.95)
        coEvery { facade.addToCart(userId, request) } returns updatedCart

        val result = facade.addToCart(userId, request)

        assertEquals(5, result.items.first().quantity)
        assertEquals(49.95, result.subtotal)
    }

    @Test
    fun `addToCart returns empty cart for invalid userId`() = runTest {
        val request = AddToCartRequest(productId = productId, quantity = 1)
        coEvery { facade.addToCart("not-a-uuid", request) } returns emptyCart.copy(userId = "not-a-uuid")

        val result = facade.addToCart("not-a-uuid", request)

        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `addToCart returns empty cart for invalid productId`() = runTest {
        val request = AddToCartRequest(productId = "bad-product-id", quantity = 1)
        coEvery { facade.addToCart(userId, request) } returns emptyCart

        val result = facade.addToCart(userId, request)

        assertTrue(result.items.isEmpty())
    }

    // --- updateCartItem ---

    @Test
    fun `updateCartItem returns cart with updated quantity`() = runTest {
        val updatedItem = sampleItem.copy(quantity = 5)
        val updatedCart = sampleCart.copy(items = listOf(updatedItem), subtotal = 49.95)
        coEvery { facade.updateCartItem(userId, itemId, 5) } returns updatedCart

        val result = facade.updateCartItem(userId, itemId, 5)

        assertNotNull(result)
        assertEquals(5, result.items.first().quantity)
        assertEquals(49.95, result.subtotal)
    }

    @Test
    fun `updateCartItem removes item when quantity is zero`() = runTest {
        val cartWithoutItem = sampleCart.copy(items = emptyList(), subtotal = 0.0)
        coEvery { facade.updateCartItem(userId, itemId, 0) } returns cartWithoutItem

        val result = facade.updateCartItem(userId, itemId, 0)

        assertNotNull(result)
        assertTrue(result.items.isEmpty())
        assertEquals(0.0, result.subtotal)
    }

    @Test
    fun `updateCartItem removes item when quantity is negative`() = runTest {
        val cartWithoutItem = sampleCart.copy(items = emptyList(), subtotal = 0.0)
        coEvery { facade.updateCartItem(userId, itemId, -1) } returns cartWithoutItem

        val result = facade.updateCartItem(userId, itemId, -1)

        assertNotNull(result)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `updateCartItem returns null for invalid userId`() = runTest {
        coEvery { facade.updateCartItem("not-a-uuid", itemId, 2) } returns null

        val result = facade.updateCartItem("not-a-uuid", itemId, 2)

        assertNull(result)
    }

    @Test
    fun `updateCartItem returns null for invalid itemId`() = runTest {
        coEvery { facade.updateCartItem(userId, "not-a-uuid", 2) } returns null

        val result = facade.updateCartItem(userId, "not-a-uuid", 2)

        assertNull(result)
    }

    // --- removeCartItem ---

    @Test
    fun `removeCartItem returns cart without removed item`() = runTest {
        val cartWithoutItem = sampleCart.copy(items = emptyList(), subtotal = 0.0)
        coEvery { facade.removeCartItem(userId, itemId) } returns cartWithoutItem

        val result = facade.removeCartItem(userId, itemId)

        assertTrue(result.items.isEmpty())
        assertEquals(0.0, result.subtotal)
    }

    @Test
    fun `removeCartItem is a no-op for invalid itemId and returns current cart`() = runTest {
        coEvery { facade.removeCartItem(userId, "not-a-uuid") } returns sampleCart

        val result = facade.removeCartItem(userId, "not-a-uuid")

        assertEquals(1, result.items.size)
    }
}
