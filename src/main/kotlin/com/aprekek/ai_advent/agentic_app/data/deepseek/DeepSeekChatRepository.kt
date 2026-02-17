package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.domain.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.ChatRequestOptions
import com.aprekek.ai_advent.agentic_app.domain.ChatRole

class DeepSeekChatRepository(
    private val apiClient: DeepSeekApiClient
) : ChatRepository {
    override suspend fun sendMessage(messages: List<ChatMessage>, options: ChatRequestOptions): String {
        val payload = messages.map { message ->
            DeepSeekMessage(
                role = message.role.toApiRole(),
                content = message.content
            )
        }

        return apiClient.sendMessages(payload, options)
    }

    private fun ChatRole.toApiRole(): String = when (this) {
        ChatRole.User -> "user"
        ChatRole.Assistant -> "assistant"
    }
}
