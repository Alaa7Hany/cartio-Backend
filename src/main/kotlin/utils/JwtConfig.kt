package com.example.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import java.util.Date

object JwtConfig {

    private const val ISSUER = "cartio-backend"
    private const val AUDIENCE = "cartio-users"
    private const val EXPIRATION_MS = 86_400_000L // 24 hours

    private val secret: String by lazy {
        System.getenv("JWT_SECRET")
            ?: throw IllegalStateException("FATAL: JWT_SECRET environment variable is missing!")
    }

    private val algorithm: Algorithm by lazy {
        Algorithm.HMAC256(secret)
    }

    fun generateToken(email: String, userId: String): String =
        JWT.create()
            .withIssuer(ISSUER)
            .withAudience(AUDIENCE)
            .withClaim("email", email)
            .withClaim("userId", userId)
            .withExpiresAt(Date(System.currentTimeMillis() + EXPIRATION_MS))
            .sign(algorithm)

    fun Application.configureSecurity() {
        authentication {
            jwt("auth-jwt") {
                realm = "cartio-backend"
                verifier(
                    JWT.require(algorithm)
                        .withIssuer(ISSUER)
                        .withAudience(AUDIENCE)
                        .build()
                )
                validate { credential ->
                    if (credential.payload.getClaim("userId").asString() != null) {
                        JWTPrincipal(credential.payload)
                    } else {
                        null
                    }
                }
            }
        }
    }
}