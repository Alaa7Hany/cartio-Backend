package com.example.database

import com.example.models.CreateProductRequest
import com.example.models.ProductResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ProductFacade {

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getAllProducts(): List<ProductResponse> =
        newSuspendedTransaction(Dispatchers.IO) {
            Products.selectAll()
                .map { row -> row.toProductResponse() }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getProductById(id: String): ProductResponse? =
        newSuspendedTransaction(Dispatchers.IO) {
            val uuid = runCatching { Uuid.parse(id) }.getOrNull() ?: return@newSuspendedTransaction null

            Products.selectAll()
                .where { Products.id eq uuid }
                .singleOrNull()
                ?.toProductResponse()
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun insertProduct(request: CreateProductRequest): ProductResponse? =
        newSuspendedTransaction(Dispatchers.IO) {
            Products.insertReturning {
                it[title] = request.title
                it[description] = request.description
                it[price] = request.price
                it[imageUrl] = request.imageUrl
                it[categoryId] = request.categoryId
            }.singleOrNull()?.toProductResponse()
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun insertProducts(requests: List<CreateProductRequest>): Boolean =
        newSuspendedTransaction(Dispatchers.IO) {
            runCatching {
                requests.forEach { request ->
                    Products.insert {
                        it[title] = request.title
                        it[description] = request.description
                        it[price] = request.price
                        it[imageUrl] = request.imageUrl
                        it[categoryId] = request.categoryId
                    }
                }
            }.onFailure { exception ->
                println("BATCH INSERT ERROR: ${exception.message}")
                exception.printStackTrace()
            }.isSuccess
        }

    suspend fun getProductsByCategory(categoryId: String): List<ProductResponse> =
        newSuspendedTransaction(Dispatchers.IO) {
            Products.selectAll()
                .where { Products.categoryId eq categoryId }
                .map { row -> row.toProductResponse() }
        }

    suspend fun getAvailableCategories(): List<String> =
        newSuspendedTransaction(Dispatchers.IO) {
            Products.select(Products.categoryId)
                .withDistinct()
                .map { it[Products.categoryId] }
        }

    @OptIn(ExperimentalUuidApi::class)
    private fun ResultRow.toProductResponse() = ProductResponse(
        id = this[Products.id].toString(),
        title = this[Products.title],
        description = this[Products.description],
        price = this[Products.price],
        imageUrl = this[Products.imageUrl],
        categoryId = this[Products.categoryId]
    )
}
