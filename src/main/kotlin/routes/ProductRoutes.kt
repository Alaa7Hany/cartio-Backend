package com.example.routes

import com.example.database.ProductFacade
import com.example.models.BaseResponse
import com.example.models.CreateProductRequest
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.productRoutes(productFacade: ProductFacade) {

    get("/products") {
        val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
        val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 20

        val result = runCatching { productFacade.getAllProducts(page, limit) }

        result.fold(
            onSuccess = { products ->
                call.respond(
                    HttpStatusCode.OK,
                    BaseResponse(
                        data = products,
                        message = "Products fetched successfully.",
                        success = true
                    )
                )
            },
            onFailure = {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    BaseResponse(
                        data = null,
                        message = "An unexpected error occurred while fetching products.",
                        success = false
                    )
                )
            }
        )
    }

    get("/products/categories") {
        val result = runCatching { productFacade.getAvailableCategories() }

        result.fold(
            onSuccess = { categories ->
                call.respond(
                    HttpStatusCode.OK,
                    BaseResponse(
                        data = categories,
                        message = "Categories fetched successfully.",
                        success = true
                    )
                )
            },
            onFailure = {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    BaseResponse(
                        data = null,
                        message = "An unexpected error occurred while fetching categories.",
                        success = false
                    )
                )
            }
        )
    }

    get("/products/search") {
        val query = call.request.queryParameters["q"]

        if (query.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                BaseResponse(
                    data = null as List<com.example.models.ProductResponse>?,
                    message = "Search query must not be empty.",
                    success = false
                )
            )
            return@get
        }

        val result = runCatching { productFacade.searchProducts(query) }

        result.fold(
            onSuccess = { products ->
                call.respond(
                    HttpStatusCode.OK,
                    BaseResponse(
                        data = products,
                        message = "Search completed successfully.",
                        success = true
                    )
                )
            },
            onFailure = {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    BaseResponse(
                        data = null as List<com.example.models.ProductResponse>?,
                        message = "An unexpected error occurred during search.",
                        success = false
                    )
                )
            }
        )
    }

    get("/products/{id}") {
        val id = call.parameters["id"]

        if (id.isNullOrBlank()) {
            call.respond(
                HttpStatusCode.BadRequest,
                BaseResponse(
                    data = null,
                    message = "Product ID must not be empty.",
                    success = false
                )
            )
            return@get
        }

        val result = runCatching { productFacade.getProductById(id) }

        result.fold(
            onSuccess = { product ->
                if (product != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        BaseResponse(
                            data = product,
                            message = "Product fetched successfully.",
                            success = true
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        BaseResponse(
                            data = null,
                            message = "No product found with ID: $id",
                            success = false
                        )
                    )
                }
            },
            onFailure = {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    BaseResponse(
                        data = null,
                        message = "An unexpected error occurred while fetching the product.",
                        success = false
                    )
                )
            }
        )
    }

        post("/products") {
            val request = call.receive<CreateProductRequest>()
            val result = runCatching { productFacade.insertProduct(request) }

            result.fold(
                onSuccess = { product ->
                    if (product != null) {
                        call.respond(
                            HttpStatusCode.Created,
                            BaseResponse(
                                data = product,
                                message = "Product created successfully.",
                                success = true
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            BaseResponse(
                                data = null,
                                message = "Product could not be created.",
                                success = false
                            )
                        )
                    }
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(
                            data = null,
                            message = "An unexpected error occurred while creating the product.",
                            success = false
                        )
                    )
                }
            )
        }

        post("/products/batch") {
            val requests = call.receive<List<CreateProductRequest>>()

            if (requests.isEmpty()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(
                        data = null,
                        message = "Request body must contain at least one product.",
                        success = false
                    )
                )
                return@post
            }

            val result = runCatching { productFacade.insertProducts(requests) }

            result.fold(
                onSuccess = { success ->
                    if (success) {
                        call.respond(
                            HttpStatusCode.Created,
                            BaseResponse(
                                data = "Batch insert successful.",
                                message = "Batch insert successful.",
                                success = true
                            )
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            BaseResponse(
                                data = null,
                                message = "Batch insert failed. No products were saved.",
                                success = false
                            )
                        )
                    }
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(
                            data = null,
                            message = "An unexpected error occurred during batch insert.",
                            success = false
                        )
                    )
                }
            )
        }

            get("/products/category/{categoryId}") {
                val categoryId = call.parameters["categoryId"]

                if (categoryId.isNullOrBlank()) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        BaseResponse(
                            data = null,
                            message = "Category ID must not be empty.",
                            success = false
                        )
                    )
                    return@get
                }

                val result = runCatching { productFacade.getProductsByCategory(categoryId) }

                result.fold(
                    onSuccess = { products ->
                        val message = if (products.isEmpty()) {
                            "No products found in category: $categoryId"
                        } else {
                            "Products fetched successfully."
                        }
                        call.respond(
                            HttpStatusCode.OK,
                            BaseResponse(
                                data = products,
                                message = message,
                                success = true
                            )
                        )
                    },
                    onFailure = {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            BaseResponse(
                                data = null,
                                message = "An unexpected error occurred while fetching products by category.",
                                success = false
                            )
                        )
                    }
                )
            }
        }
