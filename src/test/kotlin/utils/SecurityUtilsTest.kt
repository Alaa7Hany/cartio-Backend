package com.example.utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SecurityUtilsTest {

    @Test
    fun `hashPassword returns a non-empty string different from raw password`() {
        val raw = "supersecret123"
        val hashed = SecurityUtils.hashPassword(raw)

        assertTrue(hashed.isNotEmpty())
        assertNotEquals(raw, hashed)
    }

    @Test
    fun `checkPassword returns true for correct password`() {
        val raw = "supersecret123"
        val hashed = SecurityUtils.hashPassword(raw)

        assertTrue(SecurityUtils.checkPassword(raw, hashed))
    }

    @Test
    fun `checkPassword returns false for wrong password`() {
        val hashed = SecurityUtils.hashPassword("supersecret123")

        assertFalse(SecurityUtils.checkPassword("wrongpassword", hashed))
    }
}