package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.data.state.InMemoryConversationState
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SendMessageUseCaseTest {
    @Test
    fun `returns repository response for valid input`() = runTest {
        val gateway = CapturingChatGateway(responses = mutableListOf("Hello from model"))
        val useCase = SendMessageUseCase(gateway, InMemoryConversationState())

        val result = useCase(sessionId = "s1", rawInput = "Hello")

        assertTrue(result.isSuccess)
        assertEquals("Hello from model", result.getOrNull())
    }

    @Test
    fun `returns error for blank input`() = runTest {
        val gateway = CapturingChatGateway(responses = mutableListOf("ignored"))
        val useCase = SendMessageUseCase(gateway, InMemoryConversationState())

        val result = useCase(sessionId = "s1", rawInput = "   ")

        assertTrue(result.isFailure)
        assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertNull(gateway.lastSentMessages)
    }

    @Test
    fun `sends previous turn context on second request`() = runTest {
        val gateway = CapturingChatGateway(
            responses = mutableListOf("first answer", "second answer")
        )
        val useCase = SendMessageUseCase(gateway, InMemoryConversationState())

        useCase(sessionId = "s1", rawInput = "first question")
        val secondCall = useCase(sessionId = "s1", rawInput = "second question")

        assertTrue(secondCall.isSuccess)
        val secondRequestMessages = gateway.sentRequests[1]

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
        val gateway = CapturingChatGateway(responses = mutableListOf("ok"))
        val useCase = SendMessageUseCase(gateway, InMemoryConversationState())
        val options = GenerationOptions(
            maxTokens = 512,
            stopSequences = listOf("\n\n"),
            extraInstruction = "One paragraph only."
        )

        useCase(sessionId = "s1", rawInput = "question", options = options)

        assertEquals(options, gateway.sentOptions.single())
    }

    private class CapturingChatGateway(
        private val responses: MutableList<String>
    ) : ChatGateway {
        val sentRequests = mutableListOf<List<ChatMessage>>()
        val sentOptions = mutableListOf<GenerationOptions>()

        val lastSentMessages: List<ChatMessage>?
            get() = sentRequests.lastOrNull()

        override suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String {
            sentRequests += messages
            sentOptions += options
            return responses.removeFirst()
        }
    }
}
