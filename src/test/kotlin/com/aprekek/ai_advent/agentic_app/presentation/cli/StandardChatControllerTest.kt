package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.data.state.InMemoryConversationState
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.Metrics
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.port.MetricsProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.AddConversationUsageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.ClearConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.ClearConversationUsageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GetConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GetConversationUsageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendCompressedMessageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.StandardChatController
import java.io.BufferedReader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

class StandardChatControllerTest {
    @Test
    fun `handles one message then returns to mode selection`() = runTest {
        val gateway = CapturingGateway(mutableListOf("ok"))
        val conversationState = InMemoryConversationState()
        val metricsProvider = FakeMetricsProvider()
        val controller = StandardChatController(
            stdinReader = BufferedReader(StringReader("1\nhello\nq\n")),
            configProvider = FakeConfigProvider(),
            mode = ChatMode.Standard,
            sendMessageUseCase = SendMessageUseCase(gateway, conversationState),
            sendCompressedMessageUseCase = SendCompressedMessageUseCase(gateway, conversationState),
            getConversationHistoryUseCase = GetConversationHistoryUseCase(conversationState),
            clearConversationHistoryUseCase = ClearConversationHistoryUseCase(conversationState),
            getConversationUsageUseCase = GetConversationUsageUseCase(conversationState),
            addConversationUsageUseCase = AddConversationUsageUseCase(conversationState),
            clearConversationUsageUseCase = ClearConversationUsageUseCase(conversationState),
            metricsProvider = metricsProvider,
            commandParser = CommandParser(),
            consoleView = ConsoleView(),
            loadingIndicator = LoadingIndicator()
        )

        val shouldExit = controller.run()

        assertFalse(shouldExit)
        assertEquals(1, gateway.calls)
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

    private class FakeMetricsProvider : MetricsProvider {
        override fun lastMetrics(provider: ProviderType): Metrics = Metrics(
            responseTimeMs = 100,
            promptTokens = 10,
            completionTokens = 20,
            totalTokens = 30
        )
    }
}
