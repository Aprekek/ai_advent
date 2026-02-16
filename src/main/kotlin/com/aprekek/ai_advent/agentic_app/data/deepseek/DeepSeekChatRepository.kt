package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.domain.ChatRepository

class DeepSeekChatRepository(
    private val apiClient: DeepSeekApiClient
) : ChatRepository {
    override suspend fun sendMessage(userInput: String): String = apiClient.sendPrompt(userInput)
}
