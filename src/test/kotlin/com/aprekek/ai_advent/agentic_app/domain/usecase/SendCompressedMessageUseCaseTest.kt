package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.data.state.InMemoryConversationState
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class SendCompressedMessageUseCaseTest {
    @Test
    fun `keeps at most three compressed blocks plus raw turn`() = runTest {
        val gateway = StubGateway(
            responses = mutableListOf("a1", "a2", "a3", "a4", "a5", "a6")
        )
        val state = InMemoryConversationState()
        val useCase = SendCompressedMessageUseCase(gateway, state)

        repeat(6) { index ->
            val prompt = "p${index + 1}. one. two. three. four. five. six."
            val result = useCase("s1", prompt, GenerationOptions.Standard)
            assertTrue(result.isSuccess)
        }

        val compressed = useCase.compressedHistory("s1")
        val summarySize = compressed.dropLast(2).size
        assertTrue(summarySize <= 3)
        assertEquals(2, compressed.takeLast(2).size)
    }

    private class StubGateway(
        private val responses: MutableList<String>
    ) : ChatGateway {
        override suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String {
            return responses.removeFirst()
        }
    }
}
