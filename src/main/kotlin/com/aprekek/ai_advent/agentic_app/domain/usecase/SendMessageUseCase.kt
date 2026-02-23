package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState

class SendMessageUseCase(
    private val chatGateway: ChatGateway,
    private val conversationState: ConversationState,
    private val maxHistoryMessages: Int = 20
) {
    suspend operator fun invoke(
        sessionId: String,
        rawInput: String,
        options: GenerationOptions = GenerationOptions.Standard
    ): Result<String> {
        val input = rawInput.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        val userMessage = ChatMessage(role = ChatRole.User, content = input)
        val requestMessages = conversationState.history(sessionId) + userMessage

        return runCatching { chatGateway.generate(requestMessages, options).trim() }
            .mapCatching { output ->
                if (output.isBlank()) {
                    throw IllegalStateException("DeepSeek returned an empty response")
                }
                output
            }
            .onSuccess { assistantOutput ->
                conversationState.append(sessionId, userMessage)
                conversationState.append(
                    sessionId,
                    ChatMessage(role = ChatRole.Assistant, content = assistantOutput)
                )
                conversationState.trimToLast(sessionId, maxHistoryMessages)
            }
    }
}
