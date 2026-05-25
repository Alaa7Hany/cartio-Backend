package com.example.database

import com.example.models.ProductResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class FavoriteFacade {

    @OptIn(ExperimentalUuidApi::class)
    suspend fun addFavorite(userId: String, productId: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull() ?: return@newSuspendedTransaction false
            val productUuid = runCatching { Uuid.parse(productId) }.getOrNull() ?: return@newSuspendedTransaction false

            try {
                val exists = Favorites.selectAll()
                    .where { (Favorites.userId eq userUuid) and (Favorites.productId eq productUuid) }
                    .empty().not()

                if (!exists) {
                    Favorites.insert {
                        it[Favorites.userId] = userUuid
                        it[Favorites.productId] = productUuid
                    }
                }
                true
            } catch (_: Exception) {
                false
            }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun removeFavorite(userId: String, productId: String): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull() ?: return@newSuspendedTransaction false
            val productUuid = runCatching { Uuid.parse(productId) }.getOrNull() ?: return@newSuspendedTransaction false

            try {
                val deleted = Favorites.deleteWhere {
                    (Favorites.userId eq userUuid) and (Favorites.productId eq productUuid)
                }
                deleted > 0
            } catch (_: Exception) {
                false
            }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getUserFavorites(userId: String): List<ProductResponse> =
        newSuspendedTransaction(Dispatchers.IO) {
            val userUuid = runCatching { Uuid.parse(userId) }.getOrNull() ?: return@newSuspendedTransaction emptyList()

            Favorites.innerJoin(Products)
                .selectAll()
                .where { Favorites.userId eq userUuid }
                .map { row -> row.toProductResponse() }
        }

    @OptIn(ExperimentalUuidApi::class)
    private fun ResultRow.toProductResponse() = ProductResponse(
        id = this[Products.id].toString(),
        title = this[Products.title],
        description = this[Products.description],
        price = this[Products.price],
        imageUrl = this[Products.imageUrl],
        categoryId = this[Products.categoryId],
        isFeatured = this[Products.isFeatured]
    )
}





