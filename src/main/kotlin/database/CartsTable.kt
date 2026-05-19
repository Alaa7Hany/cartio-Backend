package com.example.database

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object Carts : Table("carts") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(Users.id)

    override val primaryKey = PrimaryKey(id)
}
