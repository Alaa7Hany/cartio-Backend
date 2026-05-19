package com.example.routes

import com.example.database.OrderFacade
import com.example.models.BaseResponse
import com.example.models.OrderResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.orderRoutes(orderFacade: OrderFacade) {

    authenticate("auth-jwt") {

        post("/checkout") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@post
            }

            val result = runCatching { orderFacade.checkoutCart(userId) }

            result.fold(
                onSuccess = { order ->
                    if (order != null) {
                        call.respond(
                            HttpStatusCode.OK,
                            BaseResponse(data = order, message = "Checkout successful.", success = true)
                        )
                    } else {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            BaseResponse(data = null, message = "Cannot checkout: cart is empty or not found.", success = false)
                        )
                    }
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred during checkout.", success = false)
                    )
                }
            )
        }

        get("/orders") {
            val userId = call.principal<JWTPrincipal>()?.payload?.getClaim("userId")?.asString()

            if (userId == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(data = null, message = "Unauthorized.", success = false)
                )
                return@get
            }

            val result = runCatching { orderFacade.getUserOrders(userId) }

            result.fold(
                onSuccess = { orders ->
                    val message = if (orders.isEmpty()) "No past orders found." else "Orders fetched successfully."
                    call.respond(
                        HttpStatusCode.OK,
                        BaseResponse(data = orders, message = message, success = true)
                    )
                },
                onFailure = {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        BaseResponse(data = null, message = "An unexpected error occurred while fetching orders.", success = false)
                    )
                }
            )
        }
    }
}
