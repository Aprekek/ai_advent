package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CompareTemperatureUseCaseTest {
    @Test
    fun `returns three variants with expected temperatures`() = runTest {
        val gateway = CapturingGateway(mutableListOf("t0", "t1", "t2"))
        val useCase = CompareTemperatureUseCase(gateway)

        val result = useCase("question")

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().size)
        assertEquals(listOf(0.0, 1.0, 2.0), gateway.options.map { it.temperature })
    }

    private class CapturingGateway(
        private val responses: MutableList<String>
    ) : ChatGateway {
        val options = mutableListOf<GenerationOptions>()

        override suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String {
            this.options += options
            return responses.removeFirst()
        }
    }
}
