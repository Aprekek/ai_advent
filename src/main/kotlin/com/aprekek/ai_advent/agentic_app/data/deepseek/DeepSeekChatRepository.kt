package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway

class DeepSeekChatRepository(
    private val apiClient: DeepSeekApiClient
) : ChatGateway {
    override suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String {
        return apiClient.sendMessages(messages = messages.toApiMessages(), options = options)
    }

    suspend fun generateWithContext(
        messages: List<ChatMessage>,
        options: GenerationOptions,
        requestContext: ProviderRequestContext
    ): String {
        return apiClient.sendMessages(
            messages = messages.toApiMessages(),
            options = options,
            requestContext = requestContext
        )
    }

    private fun List<ChatMessage>.toApiMessages(): List<DeepSeekMessage> = map { message ->
        DeepSeekMessage(
            role = message.role.toApiRole(),
            content = message.content
        )
    }

    private fun ChatRole.toApiRole(): String = when (this) {
        ChatRole.User -> "user"
        ChatRole.Assistant -> "assistant"
    }
}
