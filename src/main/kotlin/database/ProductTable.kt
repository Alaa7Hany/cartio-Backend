package com.example.database

import org.jetbrains.exposed.v1.core.Table
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
object Products : Table("products") {
    val id = uuid("id").autoGenerate()
    val title = varchar("title", 256)
    val description = varchar("description", 2048)
    val price = double("price")
    val imageUrl = varchar("image_url", 512)
    val categoryId = varchar("category_id", 128)

    override val primaryKey = PrimaryKey(id)
}
