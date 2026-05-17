package com.example

import com.example.utils.JwtConfig
import io.ktor.server.application.*

fun Application.configureSecurity() {
    with(JwtConfig) { configureSecurity() }
}