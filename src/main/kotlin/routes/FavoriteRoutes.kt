package com.example.routes

import com.example.database.FavoriteFacade
import com.example.models.BaseResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.favoriteRoutes(favoriteFacade: FavoriteFacade) {

    authenticate("auth-jwt") {

        get("/favorites") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@get
            }

            val result = runCatching { favoriteFacade.getUserFavorites(userId) }

            result.fold(
                onSuccess = { favorites ->
                    call.respond(
                        HttpStatusCode.OK,
                        BaseResponse(data = favorites, message = "Favorites fetched successfully.", success = true)
                    )
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while fetching favorites.", success = false)
                    )
                }
            )
        }

        post("/favorites/{productId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@post
            }

            val productId = call.parameters["productId"]

            if (productId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(data = null, message = "Product ID must not be empty.", success = false)
                )
                return@post
            }

            val result = runCatching { favoriteFacade.addFavorite(userId, productId) }

            result.fold(
                onSuccess = { success ->
                    if (success) {
                        call.respond(
                            HttpStatusCode.OK,
                            BaseResponse(data = true, message = "Product added to favorites.", success = true)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            BaseResponse(data = false, message = "Failed to add product to favorites. It may not exist.", success = false)
                        )
                    }
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while adding to favorites.", success = false)
                    )
                }
            )
        }

        delete("/favorites/{productId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@delete
            }

            val productId = call.parameters["productId"]

            if (productId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(data = null, message = "Product ID must not be empty.", success = false)
                )
                return@delete
            }

            val result = runCatching { favoriteFacade.removeFavorite(userId, productId) }

            result.fold(
                onSuccess = { success ->
                    if (success) {
                        call.respond(
                            HttpStatusCode.OK,
                            BaseResponse(data = true, message = "Product removed from favorites.", success = true)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            BaseResponse(data = false, message = "Favorite not found or could not be removed.", success = false)
                        )
                    }
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while removing from favorites.", success = false)
                    )
                }
            )
        }
    }
}

