package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class OrderItemResponse(
    val id: String,
    val productId: String,
    val title: String,
    val imageUrl: String,
    val quantity: Int,
    val priceAtCheckout: Double
)

@Serializable
data class OrderResponse(
    val id: String,
    val userId: String,
    val items: List<OrderItemResponse>,
    val totalAmount: Double,
    val orderDate: String
)
