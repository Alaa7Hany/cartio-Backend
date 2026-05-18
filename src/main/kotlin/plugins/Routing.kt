package com.example

import com.example.database.ProductFacade
import com.example.database.UserFacade
import com.example.routes.authRoutes
import com.example.routes.productRoutes
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.resources.*
import org.koin.ktor.ext.inject

fun Application.configureRouting() {
    val userFacade by inject<UserFacade>()
    val productFacade by inject<ProductFacade>()

    routing {
        authRoutes(userFacade)
        productRoutes(productFacade)
        get("/") {
            call.respondText("Hello, World!")
        }
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}