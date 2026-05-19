package com.example.routes

import com.example.database.UserFacade
import com.example.models.BaseResponse
import com.example.models.LoginRequest
import com.example.models.RegisterRequest
import com.example.models.UserResponse
import com.example.utils.JwtConfig
import com.example.utils.SecurityUtils
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun Route.authRoutes(userFacade: UserFacade) {

    authenticate("auth-jwt") {
        get("/auth/me") {
            val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email")?.asString()

            if (email == null) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    BaseResponse(
                        data = null,
                        message = "Unauthorized.",
                        success = false
                    )
                )
                return@get
            }

            val user = userFacade.getUserByEmail(email)

            if (user == null) {
                call.respond(
                    HttpStatusCode.NotFound,
                    BaseResponse(
                        data = null,
                        message = "User not found.",
                        success = false
                    )
                )
                return@get
            }

            val token = JwtConfig.generateToken(email = user.email, userId = user.id)

            val userResponse = UserResponse(
                id = user.id,
                email = user.email,
                fullName = user.fullName,
                createdAt = user.createdAt,
                token = token
            )

            call.respond(
                HttpStatusCode.OK,
                BaseResponse(
                    data = userResponse,
                    message = "Session restored successfully.",
                    success = true
                )
            )
        }
    }

    post("/register") {
        val request = call.receive<RegisterRequest>()
        val passwordHash = SecurityUtils.hashPassword(request.password)

        val result = runCatching { userFacade.insertUser(request, passwordHash) }

        result.fold(
            onSuccess = { userResponse ->
                if (userResponse != null) {
                    call.respond(
                        HttpStatusCode.Created,
                        BaseResponse(
                            data = userResponse,
                            message = "User registered successfully.",
                            success = true
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        BaseResponse(
                            data = null,
                            message = "Registration failed. Email may already be in use.",
                            success = false
                        )
                    )
                }
            },
            onFailure = { exception ->
                val isDuplicateEmail = exception.message?.contains("users_email_unique") == true
                        || exception.message?.contains("duplicate key") == true

                val cleanMessage = if (isDuplicateEmail) {
                    "This email is already registered. Please log in."
                } else {
                    "An unexpected error occurred during registration."
                }
                val statusCode = if (isDuplicateEmail) HttpStatusCode.Conflict else HttpStatusCode.BadRequest

                call.respond(
                    statusCode,
                    BaseResponse(
                        data = null,
                        message = cleanMessage,
                        success = false
                    )
                )
            }
        )
    }

    post("/login") {
        val request = call.receive<LoginRequest>()

        val user = userFacade.getUserByEmail(request.email)

        if (user == null) {
            call.respond(
                HttpStatusCode.Unauthorized,
                BaseResponse(
                    data = null,
                    message = "Invalid email or password.",
                    success = false
                )
            )
            return@post
        }

        if (!SecurityUtils.checkPassword(request.password, user.passwordHash)) {
            call.respond(
                HttpStatusCode.Unauthorized,
                BaseResponse(
                    data = null,
                    message = "Invalid email or password.",
                    success = false
                )
            )
            return@post
        }

        val token = JwtConfig.generateToken(email = user.email, userId = user.id)

        val userResponse = UserResponse(
            id = user.id,
            email = user.email,
            fullName = user.fullName,
            createdAt = user.createdAt,
            token = token
        )

        call.respond(
            HttpStatusCode.OK,
            BaseResponse(
                data = userResponse,
                message = "Login successful.",
                success = true
            )
        )
    }
}
