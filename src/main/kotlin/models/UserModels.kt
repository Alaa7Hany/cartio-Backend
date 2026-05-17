package com.example.models
import kotlinx.serialization.Serializable

@Serializable
data class BaseResponse<T>(
    val data: T?,
    val message: String?,
    val success: Boolean
)



@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val fullName: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val fullName: String,
    val createdAt: String,
    val token: String? = null
)

data class AuthUser(
    val id: String,
    val email: String,
    val passwordHash: String,
    val fullName: String,
    val createdAt: String
)
