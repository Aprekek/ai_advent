package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekChatRepository
import com.aprekek.ai_advent.agentic_app.domain.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.ChatRequestOptions
import com.aprekek.ai_advent.agentic_app.domain.ChatRole

class SingleRequestExecutor(
    private val chatRepository: DeepSeekChatRepository
) {
    suspend fun execute(
        prompt: String,
        options: ChatRequestOptions = ChatRequestOptions.Standard
    ): Result<String> {
        val input = prompt.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }
        return runCatching {
            chatRepository.sendMessage(
                messages = listOf(ChatMessage(role = ChatRole.User, content = input)),
                options = options
            ).trim()
        }.mapCatching { output ->
            if (output.isBlank()) {
                throw IllegalStateException("DeepSeek returned an empty response")
            }
            output
        }
    }
}
