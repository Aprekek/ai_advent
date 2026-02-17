package com.aprekek.ai_advent.agentic_app.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

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

    @Test
    fun `passes request options to repository`() = runTest {
        val repository = CapturingChatRepository(responses = mutableListOf("ok"))
        val useCase = SendMessageUseCase(repository)
        val options = ChatRequestOptions(
            maxTokens = 512,
            stopSequences = listOf("\n\n"),
            extraSystemInstruction = "One paragraph only."
        )

        useCase("question", options)

        assertEquals(options, repository.sentOptions.single())
    }

    private class CapturingChatRepository(
        private val responses: MutableList<String>
    ) : ChatRepository {
        val sentRequests = mutableListOf<List<ChatMessage>>()
        val sentOptions = mutableListOf<ChatRequestOptions>()

        val lastSentMessages: List<ChatMessage>?
            get() = sentRequests.lastOrNull()

        override suspend fun sendMessage(messages: List<ChatMessage>, options: ChatRequestOptions): String {
            sentRequests += messages
            sentOptions += options
            return responses.removeFirst()
        }
    }
}
