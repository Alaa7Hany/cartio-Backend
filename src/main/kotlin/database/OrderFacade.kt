package com.example.database

import com.example.models.OrderItemResponse
import com.example.models.OrderResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class OrderFacade {

    suspend fun checkoutCart(userId: String): OrderResponse? =
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull()
                ?: return@newSuspendedTransaction null

            val cartId = Carts.selectAll()
                .where { Carts.userId eq userUuid }
                .singleOrNull()
                ?.get(Carts.id)
                ?: return@newSuspendedTransaction null

            val cartRows = CartItems
                .join(Products, JoinType.INNER, onColumn = CartItems.productId, otherColumn = Products.id)
                .selectAll()
                .where { CartItems.cartId eq cartId }
                .toList()

            if (cartRows.isEmpty()) return@newSuspendedTransaction null

            val totalAmount = cartRows.sumOf { it[CartItems.quantity] * it[Products.price] }
            val orderDate = Instant.now().toString()

            val orderId = Orders.insertReturning {
                it[Orders.userId] = userUuid
                it[Orders.totalAmount] = totalAmount
                it[Orders.orderDate] = orderDate
            }.singleOrNull()?.get(Orders.id)
                ?: return@newSuspendedTransaction null

            val orderItems = cartRows.map { row ->
                val orderItemId = OrderItems.insertReturning {
                    it[OrderItems.orderId] = orderId
                    it[OrderItems.productId] = row[Products.id]
                    it[OrderItems.quantity] = row[CartItems.quantity]
                    it[OrderItems.priceAtCheckout] = row[Products.price]
                }.singleOrNull()?.get(OrderItems.id)
                    ?: return@newSuspendedTransaction null

                row.toOrderItemResponse(
                    overrideId = orderItemId.toString(),
                    price = row[Products.price],
                    quantity = row[CartItems.quantity]
                )
            }

            CartItems.deleteWhere { CartItems.cartId eq cartId }

            OrderResponse(
                id = orderId.toString(),
                userId = userId,
                items = orderItems,
                totalAmount = totalAmount,
                orderDate = orderDate
            )
        }

    suspend fun getUserOrders(userId: String): List<OrderResponse> =
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull()
                ?: return@newSuspendedTransaction emptyList()

            val orders = Orders.selectAll()
                .where { Orders.userId eq userUuid }
                .orderBy(Orders.orderDate, SortOrder.DESC)
                .toList()

            orders.map { orderRow ->
                val orderId = orderRow[Orders.id]

                val items = OrderItems
                    .join(Products, JoinType.INNER, onColumn = OrderItems.productId, otherColumn = Products.id)
                    .selectAll()
                    .where { OrderItems.orderId eq orderId }
                    .map {
                        it.toOrderItemResponse(
                            price = it[OrderItems.priceAtCheckout],
                            quantity = it[OrderItems.quantity]
                        )
                    }
                OrderResponse(
                    id = orderId.toString(),
                    userId = userId,
                    items = items,
                    totalAmount = orderRow[Orders.totalAmount],
                    orderDate = orderRow[Orders.orderDate]
                )
            }
        }

    private fun ResultRow.toOrderItemResponse(
        overrideId: String? = null,
        price: Double,
        quantity: Int
    ) = OrderItemResponse(
        id = overrideId ?: this[OrderItems.id].toString(),
        productId = this[Products.id].toString(),
        title = this[Products.title],
        imageUrl = this[Products.imageUrl],
        quantity = quantity, // Use the parameter here!
        priceAtCheckout = price
    )
}
