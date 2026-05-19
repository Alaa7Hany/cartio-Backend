package com.example.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object OrderItems : Table("order_items") {
    val id = uuid("id").autoGenerate()
    val orderId = uuid("order_id").references(Orders.id, onDelete = ReferenceOption.CASCADE)
    val productId = uuid("product_id").references(Products.id)
    val quantity = integer("quantity")
    val priceAtCheckout = double("price_at_checkout")

    override val primaryKey = PrimaryKey(id)
}
