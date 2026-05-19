package com.example

import com.example.database.CartFacade
import com.example.database.ProductFacade
import com.example.database.UserFacade
import io.ktor.server.application.*
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(module {
            single { UserFacade() }
            single { ProductFacade() }
            single { CartFacade() }
        })
    }
}
