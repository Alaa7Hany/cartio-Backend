package com.example.database

import com.example.models.AuthUser
import com.example.models.RegisterRequest
import com.example.models.UserResponse
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.uuid.ExperimentalUuidApi

class UserFacade {

    @OptIn(ExperimentalUuidApi::class)
    suspend fun insertUser(request: RegisterRequest, passwordHash: String): UserResponse? =
        newSuspendedTransaction(Dispatchers.IO) {
            val now = java.time.Instant.now().toString()

            Users.insertReturning {
                it[email] = request.email
                it[Users.passwordHash] = passwordHash
                it[fullName] = request.fullName
                it[createdAt] = now
            }.singleOrNull()?.let { row ->
                UserResponse(
                    id = row[Users.id].toString(),
                    email = row[Users.email],
                    fullName = row[Users.fullName],
                    createdAt = row[Users.createdAt]
                )
            }
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend fun getUserByEmail(email: String): AuthUser? =
        newSuspendedTransaction(Dispatchers.IO) {
            Users.selectAll()
                .where { Users.email eq email }
                .singleOrNull()
                ?.let { row ->
                    AuthUser(
                        id = row[Users.id].toString(),
                        email = row[Users.email],
                        passwordHash = row[Users.passwordHash],
                        fullName = row[Users.fullName],
                        createdAt = row[Users.createdAt]
                    )
                }
        }
}
