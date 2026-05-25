package com.example

import com.example.database.*
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configurePostgres() {
    val dbDriver = environment.config.propertyOrNull("database.driver")?.getString() ?: "org.postgresql.Driver"

    val rawDbUrl = System.getenv("DB_URL")
        ?: throw IllegalStateException("FATAL: DB_URL environment variable is missing!")
    val dbUser = System.getenv("DB_USER")
        ?: throw IllegalStateException("FATAL: DB_USER environment variable is missing!")
    val dbPassword = System.getenv("DB_PASSWORD")
        ?: throw IllegalStateException("FATAL: DB_PASSWORD environment variable is missing!")

    val baseJdbcUrl = "$rawDbUrl?user=$dbUser&sslmode=require"

    val safeDbUrl = if (baseJdbcUrl.contains("prepareThreshold")) {
        baseJdbcUrl
    } else {
        "$baseJdbcUrl&prepareThreshold=0"
    }

    if (!DatabaseHolder.isInitialized) {
        Database.connect(
            url = safeDbUrl,
            driver = dbDriver,
            password = dbPassword
        )
        DatabaseHolder.isInitialized = true
    }

    transaction {
        SchemaUtils.create(Users, Products, Carts, CartItems, Orders, OrderItems, Favorites)
    }

    println("Successfully connected to Supabase Database!")
}

private object DatabaseHolder {
    var isInitialized: Boolean = false
}