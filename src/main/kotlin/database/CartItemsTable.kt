package com.example.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object CartItems : Table("cart_items") {
    val id = uuid("id").autoGenerate()
    val cartId = uuid("cart_id").references(Carts.id, onDelete = ReferenceOption.CASCADE)
    val productId = uuid("product_id").references(Products.id)
    val quantity = integer("quantity")

    override val primaryKey = PrimaryKey(id)
}
