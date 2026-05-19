package com.example

import com.example.database.CartItems
import com.example.database.Carts
import com.example.database.Products
import com.example.database.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun Application.configurePostgres() {
    val dbUrl = environment.config.property("database.jdbcUrl").getString()
    val dbDriver = environment.config.property("database.driver").getString()

    val dbPassword = System.getenv("DB_PASSWORD")
        ?: throw IllegalStateException("FATAL: DB_PASSWORD environment variable is missing!")

    // Append prepareThreshold=0 to disable server-side prepared statement caching,
    // preventing "prepared statement already exists" conflicts on reconnection.
    val safeDbUrl = if (dbUrl.contains("prepareThreshold")) {
        dbUrl
    } else {
        val separator = if (dbUrl.contains("?")) "&" else "?"
        "$dbUrl${separator}prepareThreshold=0"
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
        SchemaUtils.create(Users, Products, Carts, CartItems)
    }

    println("Successfully connected to Supabase Database!")
}

private object DatabaseHolder {
    var isInitialized: Boolean = false
}