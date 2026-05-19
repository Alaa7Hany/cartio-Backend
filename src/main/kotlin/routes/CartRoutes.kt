package com.example.routes

import com.example.database.CartFacade
import com.example.models.AddToCartRequest
import com.example.models.BaseResponse
import com.example.models.UpdateCartItemRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.cartRoutes(cartFacade: CartFacade) {

    authenticate("auth-jwt") {

        get("/cart") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@get
            }

            val result = runCatching { cartFacade.getCartForUser(userId) }

            result.fold(
                onSuccess = { cart ->
                    call.respond(
                        HttpStatusCode.OK,
                        BaseResponse(data = cart, message = "Cart fetched successfully.", success = true)
                    )
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while fetching the cart.", success = false)
                    )
                }
            )
        }

        post("/cart/add") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@post
            }

            val request = runCatching { call.receive<AddToCartRequest>() }.getOrElse {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(data = null, message = "Invalid request body.", success = false)
                )
                return@post
            }

            val result = runCatching { cartFacade.addToCart(userId, request) }

            result.fold(
                onSuccess = { cart ->
                    call.respond(
                        HttpStatusCode.OK,
                        BaseResponse(data = cart, message = "Item added to cart successfully.", success = true)
                    )
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while adding item to cart.", success = false)
                    )
                }
            )
        }

        put("/cart/{itemId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@put
            }

            val itemId = call.parameters["itemId"]

            if (itemId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(data = null, message = "Item ID must not be empty.", success = false)
                )
                return@put
            }

            val request = runCatching { call.receive<UpdateCartItemRequest>() }.getOrElse {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(data = null, message = "Invalid request body.", success = false)
                )
                return@put
            }

            val result = runCatching { cartFacade.updateCartItem(userId, itemId, request.quantity) }

            result.fold(
                onSuccess = { cart ->
                    if (cart != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            BaseResponse(data = cart, message = "Cart item updated successfully.", success = true)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.NotFound,
                            BaseResponse(data = null, message = "Cart item not found.", success = false)
                        )
                    }
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while updating the cart item.", success = false)
                    )
                }
            )
        }

        delete("/cart/{itemId}") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@delete
            }

            val itemId = call.parameters["itemId"]

            if (itemId.isNullOrBlank()) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    BaseResponse(data = null, message = "Item ID must not be empty.", success = false)
                )
                return@delete
            }

            val result = runCatching { cartFacade.removeCartItem(userId, itemId) }

            result.fold(
                onSuccess = { cart ->
                    call.respond(
                        HttpStatusCode.OK,
                        BaseResponse(data = cart, message = "Cart item removed successfully.", success = true)
                    )
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while removing the cart item.", success = false)
                    )
                }
            )
        }
    }
}
