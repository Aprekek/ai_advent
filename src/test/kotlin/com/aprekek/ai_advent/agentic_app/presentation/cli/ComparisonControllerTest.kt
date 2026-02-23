package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.ComparePromptStrategiesUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ComparisonController
import java.io.BufferedReader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class ComparisonControllerTest {
    @Test
    fun `runs comparison variants and returns to mode selection`() = runTest {
        val gateway = CapturingGateway(
            mutableListOf(
                "plain",
                "step",
                "generated prompt",
                "generated result",
                "experts"
            )
        )

        val controller = ComparisonController(
            stdinReader = BufferedReader(StringReader("task\n\n\n\n")),
            configProvider = FakeConfigProvider(),
            mode = ChatMode.Comparison,
            commandParser = CommandParser(),
            consoleView = ConsoleView(),
            loadingIndicator = LoadingIndicator(),
            comparePromptStrategiesUseCase = ComparePromptStrategiesUseCase(gateway)
        )

        val shouldExit = controller.run()

        assertFalse(shouldExit)
        assertEquals(5, gateway.calls)
    }

    private class CapturingGateway(
        private val responses: MutableList<String>
    ) : ChatGateway {
        var calls: Int = 0

        override suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String {
            calls++
            return responses.removeFirst()
        }
    }

    private class FakeConfigProvider : ConfigProvider {
        override fun model(): String = "deepseek-chat"
        override fun responseLanguage(): String = "Russian"
        override fun huggingFaceModelV30(): String = "deepseek-ai/DeepSeek-V3:novita"
        override fun hasHuggingFaceApiKey(): Boolean = true
    }
}
