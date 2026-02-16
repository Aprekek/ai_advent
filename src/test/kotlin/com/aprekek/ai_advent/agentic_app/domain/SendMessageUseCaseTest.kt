package com.aprekek.ai_advent.agentic_app.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SendMessageUseCaseTest {
    @Test
    fun `returns repository response for valid input`() = runTest {
        val useCase = SendMessageUseCase(FakeChatRepository(response = "Hello from model"))

        val result = useCase("Hello")

        assertTrue(result.isSuccess)
        assertEquals("Hello from model", result.getOrNull())
    }

    @Test
    fun `returns error for blank input`() = runTest {
        val useCase = SendMessageUseCase(FakeChatRepository(response = "ignored"))

        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
    }

    private class FakeChatRepository(
        private val response: String
    ) : ChatRepository {
        override suspend fun sendMessage(userInput: String): String = response
    }
}
