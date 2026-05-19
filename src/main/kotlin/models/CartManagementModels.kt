package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class AddToCartRequest(
    val productId: String,
    val quantity: Int
)

@Serializable
data class UpdateCartItemRequest(
    val quantity: Int
)

@Serializable
data class CartItemResponse(
    val id: String,
    val productId: String,
    val title: String,
    val price: Double,
    val imageUrl: String,
    val quantity: Int
)

@Serializable
data class CartResponse(
    val id: String,
    val userId: String,
    val items: List<CartItemResponse>,
    val subtotal: Double
)
