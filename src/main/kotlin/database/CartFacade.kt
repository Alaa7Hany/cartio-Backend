package com.example.database

import com.example.models.AddToCartRequest
import com.example.models.CartItemResponse
import com.example.models.CartResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class CartFacade {

    suspend fun getCartForUser(userId: String): CartResponse =
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull()
                ?: return@newSuspendedTransaction emptyCartResponse(userId)

            val cartRow = Carts.selectAll()
                .where { Carts.userId eq userUuid }
                .singleOrNull()

            if (cartRow == null) {
                val newCartId = Carts.insertReturning {
                    it[Carts.userId] = userUuid
                }.singleOrNull()?.get(Carts.id)
                    ?: return@newSuspendedTransaction emptyCartResponse(userId)

                return@newSuspendedTransaction CartResponse(
                    id = newCartId.toString(),
                    userId = userId,
                    items = emptyList(),
                    subtotal = 0.0
                )
            }

            val cartId = cartRow[Carts.id]

            val items = CartItems
                .join(Products, JoinType.INNER, onColumn = CartItems.productId, otherColumn = Products.id)
                .selectAll()
                .where { CartItems.cartId eq cartId }
                .map { it.toCartItemResponse() }

            CartResponse(
                id = cartId.toString(),
                userId = userId,
                items = items,
                subtotal = items.sumOf { it.price * it.quantity }
            )
        }

    suspend fun addToCart(userId: String, request: AddToCartRequest): CartResponse {
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull() ?: return@newSuspendedTransaction
            val productUuid = runCatching { Uuid.parse(request.productId) }.getOrNull() ?: return@newSuspendedTransaction

            val cartId = Carts.selectAll()
                .where { Carts.userId eq userUuid }
                .singleOrNull()
                ?.get(Carts.id)
                ?: Carts.insertReturning { it[Carts.userId] = userUuid }
                    .singleOrNull()?.get(Carts.id)
                ?: return@newSuspendedTransaction

            val existingItem = CartItems.selectAll()
                .where { (CartItems.cartId eq cartId) and (CartItems.productId eq productUuid) }
                .singleOrNull()

            if (existingItem != null) {
                val updatedQuantity = existingItem[CartItems.quantity] + request.quantity
                CartItems.update({ (CartItems.cartId eq cartId) and (CartItems.productId eq productUuid) }) {
                    it[quantity] = updatedQuantity
                }
            } else {
                CartItems.insert {
                    it[CartItems.cartId] = cartId
                    it[CartItems.productId] = productUuid
                    it[quantity] = request.quantity
                }
            }
        }

        return getCartForUser(userId)
    }

    suspend fun updateCartItem(userId: String, itemId: String, quantity: Int): CartResponse? {
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull() ?: return@newSuspendedTransaction
            val itemUuid = runCatching { Uuid.parse(itemId) }.getOrNull() ?: return@newSuspendedTransaction

            val cartId = Carts.selectAll()
                .where { Carts.userId eq userUuid }
                .singleOrNull()
                ?.get(Carts.id)
                ?: return@newSuspendedTransaction

            if (quantity <= 0) {
                CartItems.deleteWhere {
                    (CartItems.id eq itemUuid) and (CartItems.cartId eq cartId)
                }
            } else {
                CartItems.update({ (CartItems.id eq itemUuid) and (CartItems.cartId eq cartId) }) {
                    it[CartItems.quantity] = quantity
                }
            }
        }

        return getCartForUser(userId)
    }

    suspend fun removeCartItem(userId: String, itemId: String): CartResponse {
        newSuspendedTransaction(Dispatchers.IO) {
            val itemUuid = runCatching { Uuid.parse(itemId) }.getOrNull()

            if (itemUuid != null) {
                CartItems.deleteWhere { CartItems.id eq itemUuid }
            }
        }

        return getCartForUser(userId)
    }

    private fun emptyCartResponse(userId: String) = CartResponse(
        id = "",
        userId = userId,
        items = emptyList(),
        subtotal = 0.0
    )

    private fun ResultRow.toCartItemResponse() = CartItemResponse(
        id = this[CartItems.id].toString(),
        productId = this[Products.id].toString(),
        title = this[Products.title],
        price = this[Products.price],
        imageUrl = this[Products.imageUrl],
        quantity = this[CartItems.quantity]
    )
}
