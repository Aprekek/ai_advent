package com.aprekek.ai_advent.agentic_app.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SendMessageUseCaseTest {
    @Test
    fun `returns repository response for valid input`() = runTest {
        val repository = CapturingChatRepository(responses = mutableListOf("Hello from model"))
        val useCase = SendMessageUseCase(repository)

        val result = useCase("Hello")

        assertTrue(result.isSuccess)
        assertEquals("Hello from model", result.getOrNull())
    }

    @Test
    fun `returns error for blank input`() = runTest {
        val repository = CapturingChatRepository(responses = mutableListOf("ignored"))
        val useCase = SendMessageUseCase(repository)

        val result = useCase("   ")

        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertNull(repository.lastSentMessages)
    }

    @Test
    fun `sends previous turn context on second request`() = runTest {
        val repository = CapturingChatRepository(
            responses = mutableListOf("first answer", "second answer")
        )
        val useCase = SendMessageUseCase(repository)

        useCase("first question")
        val secondCall = useCase("second question")

        assertTrue(secondCall.isSuccess)
        val secondRequestMessages = repository.sentRequests[1]

        assertEquals(3, secondRequestMessages.size)
        assertEquals(ChatRole.User, secondRequestMessages[0].role)
        assertEquals("first question", secondRequestMessages[0].content)
        assertEquals(ChatRole.Assistant, secondRequestMessages[1].role)
        assertEquals("first answer", secondRequestMessages[1].content)
        assertEquals(ChatRole.User, secondRequestMessages[2].role)
        assertEquals("second question", secondRequestMessages[2].content)
    }

    private class CapturingChatRepository(
        private val responses: MutableList<String>
    ) : ChatRepository {
        val sentRequests = mutableListOf<List<ChatMessage>>()

        val lastSentMessages: List<ChatMessage>?
            get() = sentRequests.lastOrNull()

        override suspend fun sendMessage(messages: List<ChatMessage>): String {
            sentRequests += messages
            return responses.removeFirst()
        }
    }
}
