package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.data.state.InMemoryConversationState
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.ClearConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GetConversationHistoryUseCase
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
        val controller = StandardChatController(
            stdinReader = BufferedReader(StringReader("hello\nq\n")),
            configProvider = FakeConfigProvider(),
            mode = ChatMode.Standard,
            sendMessageUseCase = SendMessageUseCase(gateway, conversationState),
            getConversationHistoryUseCase = GetConversationHistoryUseCase(conversationState),
            clearConversationHistoryUseCase = ClearConversationHistoryUseCase(conversationState),
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
}
