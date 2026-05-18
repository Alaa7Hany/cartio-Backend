package com.example

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

    Database.connect(
        url = dbUrl,
        driver = dbDriver,
        password = dbPassword
    )
    transaction {
        SchemaUtils.create(Users)
        SchemaUtils.create(Users, Products)
    }
    println("Successfully connected to Supabase Database!")
}