package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekChatRepository
import com.aprekek.ai_advent.agentic_app.data.deepseek.ProviderRequestContext
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions

class SingleRequestExecutor(
    private val chatRepository: DeepSeekChatRepository
) {
    suspend fun execute(
        prompt: String,
        options: GenerationOptions = GenerationOptions.Standard,
        requestContext: ProviderRequestContext? = null
    ): Result<String> {
        val input = prompt.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        val messages = listOf(ChatMessage(role = ChatRole.User, content = input))
        return runCatching {
            if (requestContext == null) {
                chatRepository.generate(messages = messages, options = options)
            } else {
                chatRepository.generateWithContext(
                    messages = messages,
                    options = options,
                    requestContext = requestContext
                )
            }.trim()
        }.mapCatching { output ->
            if (output.isBlank()) {
                throw IllegalStateException("DeepSeek returned an empty response")
            }
            output
        }
    }
}
