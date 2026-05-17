package com.example.database

import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

object Users : Table("users") {
    @OptIn(ExperimentalUuidApi::class)
    val id = uuid("id").autoGenerate()
    val email = varchar("email", 256).uniqueIndex()
    val passwordHash = varchar("password_hash", 512)
    val fullName = varchar("full_name", 256)
    val createdAt = varchar("created_at", 64)

    @OptIn(ExperimentalUuidApi::class)
    override val primaryKey = PrimaryKey(id)
}
