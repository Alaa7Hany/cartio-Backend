package com.example.database

import com.example.models.RegisterRequest
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserFacadeTest {

    private val userFacade: UserFacade = mockk()

    private val validRequest = RegisterRequest(
        email = "john@example.com",
        password = "plaintext",
        fullName = "John Doe"
    )

    @Test
    fun `insertUser returns UserResponse on success`() = runTest {
        coEvery { userFacade.insertUser(validRequest, any()) } returns com.example.models.UserResponse(
            id = "some-uuid",
            email = validRequest.email,
            fullName = validRequest.fullName,
            createdAt = "2026-01-01T00:00:00Z"
        )

        val result = userFacade.insertUser(validRequest, "hashed_password")

        assertNotNull(result)
        assert(result.email == validRequest.email)
        assert(result.fullName == validRequest.fullName)
    }

    @Test
    fun `insertUser returns null on duplicate email`() = runTest {
        coEvery { userFacade.insertUser(validRequest, any()) } returns null

        val result = userFacade.insertUser(validRequest, "hashed_password")

        assertNull(result)
    }
}
