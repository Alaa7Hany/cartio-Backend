package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class ProductResponse(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val categoryId: String,
    val isFeatured: Boolean
)

@Serializable
data class CreateProductRequest(
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val categoryId: String,
    val isFeatured: Boolean = false
)
