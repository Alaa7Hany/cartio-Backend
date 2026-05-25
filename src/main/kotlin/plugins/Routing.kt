package com.example

import com.example.database.CartFacade
import com.example.database.FavoriteFacade
import com.example.database.OrderFacade
import com.example.database.ProductFacade
import com.example.database.UserFacade
import com.example.routes.authRoutes
import com.example.routes.cartRoutes
import com.example.routes.favoriteRoutes
import com.example.routes.orderRoutes
import com.example.routes.productRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val userFacade by inject<UserFacade>()
    val productFacade by inject<ProductFacade>()
    val cartFacade by inject<CartFacade>()
    val orderFacade by inject<OrderFacade>()
    val favoriteFacade by inject<FavoriteFacade>()

    routing {
        authRoutes(userFacade)
        productRoutes(productFacade)
        cartRoutes(cartFacade)
        orderRoutes(orderFacade)
        favoriteRoutes(favoriteFacade)
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}