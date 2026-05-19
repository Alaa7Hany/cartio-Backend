package com.example.database

import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object Orders : Table("orders") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(Users.id)
    val totalAmount = double("total_amount")
    val orderDate = varchar("order_date", 64)

    override val primaryKey = PrimaryKey(id)
}
