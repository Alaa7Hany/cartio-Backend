package com.example.database

import com.example.models.OrderItemResponse
import com.example.models.OrderResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderFacadeTest {

    private val facade: OrderFacade = mockk()

    private val userId = "550e8400-e29b-41d4-a716-446655440000"
    private val orderId = "aa0e8400-e29b-41d4-a716-446655440010"
    private val orderItemId = "bb0e8400-e29b-41d4-a716-446655440011"
    private val productId = "660e8400-e29b-41d4-a716-446655440001"

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

    // --- checkoutCart ---

    @Test
    fun `checkoutCart returns OrderResponse when cart has items`() = runTest {
        coEvery { facade.checkoutCart(userId) } returns sampleOrder

        val result = facade.checkoutCart(userId)

        assertNotNull(result)
        assertEquals(orderId, result.id)
        assertEquals(userId, result.userId)
        assertEquals(19.98, result.totalAmount)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `checkoutCart clears cart and sets priceAtCheckout from product price at time of order`() = runTest {
        coEvery { facade.checkoutCart(userId) } returns sampleOrder

        val result = facade.checkoutCart(userId)

        assertNotNull(result)
        assertEquals(9.99, result.items.first().priceAtCheckout)
    }

    @Test
    fun `checkoutCart returns null when cart is empty`() = runTest {
        coEvery { facade.checkoutCart(userId) } returns null

        val result = facade.checkoutCart(userId)

        assertNull(result)
    }

    @Test
    fun `checkoutCart returns null when cart does not exist for user`() = runTest {
        val unknownUserId = "cc0e8400-e29b-41d4-a716-000000000000"
        coEvery { facade.checkoutCart(unknownUserId) } returns null

        val result = facade.checkoutCart(unknownUserId)

        assertNull(result)
    }

    @Test
    fun `checkoutCart returns null for invalid userId UUID`() = runTest {
        coEvery { facade.checkoutCart("not-a-uuid") } returns null

        val result = facade.checkoutCart("not-a-uuid")

        assertNull(result)
    }

    @Test
    fun `checkoutCart totalAmount equals sum of quantity times priceAtCheckout`() = runTest {
        val multiItemOrder = sampleOrder.copy(
            items = listOf(
                sampleOrderItem.copy(quantity = 3, priceAtCheckout = 10.00),
                sampleOrderItem.copy(id = "cc000000-0000-0000-0000-000000000001", quantity = 1, priceAtCheckout = 5.00)
            ),
            totalAmount = 35.00
        )
        coEvery { facade.checkoutCart(userId) } returns multiItemOrder

        val result = facade.checkoutCart(userId)

        assertNotNull(result)
        val expectedTotal = result.items.sumOf { it.quantity * it.priceAtCheckout }
        assertEquals(expectedTotal, result.totalAmount)
    }

    // --- getUserOrders ---

    @Test
    fun `getUserOrders returns list of orders for known user`() = runTest {
        coEvery { facade.getUserOrders(userId) } returns listOf(sampleOrder)

        val result = facade.getUserOrders(userId)

        assertEquals(1, result.size)
        assertEquals(orderId, result.first().id)
        assertEquals(userId, result.first().userId)
    }

    @Test
    fun `getUserOrders returns empty list when user has no orders`() = runTest {
        coEvery { facade.getUserOrders(userId) } returns emptyList()

        val result = facade.getUserOrders(userId)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUserOrders returns empty list for invalid userId UUID`() = runTest {
        coEvery { facade.getUserOrders("not-a-uuid") } returns emptyList()

        val result = facade.getUserOrders("not-a-uuid")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getUserOrders uses priceAtCheckout not current product price`() = runTest {
        val historicalOrder = sampleOrder.copy(
            items = listOf(sampleOrderItem.copy(priceAtCheckout = 5.00))
        )
        coEvery { facade.getUserOrders(userId) } returns listOf(historicalOrder)

        val result = facade.getUserOrders(userId)

        assertEquals(5.00, result.first().items.first().priceAtCheckout)
    }

    @Test
    fun `getUserOrders returns orders sorted by orderDate descending`() = runTest {
        val olderOrder = sampleOrder.copy(id = "dd000000-0000-0000-0000-000000000001", orderDate = "2026-01-01T00:00:00Z")
        val newerOrder = sampleOrder.copy(id = "ee000000-0000-0000-0000-000000000002", orderDate = "2026-05-19T10:00:00Z")
        coEvery { facade.getUserOrders(userId) } returns listOf(newerOrder, olderOrder)

        val result = facade.getUserOrders(userId)

        assertEquals("2026-05-19T10:00:00Z", result.first().orderDate)
        assertEquals("2026-01-01T00:00:00Z", result.last().orderDate)
    }

    @Test
    fun `getUserOrders includes full item details per order`() = runTest {
        coEvery { facade.getUserOrders(userId) } returns listOf(sampleOrder)

        val result = facade.getUserOrders(userId)
        val item = result.first().items.first()

        assertEquals(orderItemId, item.id)
        assertEquals(productId, item.productId)
        assertEquals("Test Product", item.title)
        assertEquals("https://example.com/image.png", item.imageUrl)
        assertEquals(2, item.quantity)
        assertEquals(9.99, item.priceAtCheckout)
    }
}
