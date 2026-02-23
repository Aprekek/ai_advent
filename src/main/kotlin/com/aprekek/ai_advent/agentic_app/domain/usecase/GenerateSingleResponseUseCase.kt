package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway

class GenerateSingleResponseUseCase(
    private val chatGateway: ChatGateway
) {
    suspend operator fun invoke(
        prompt: String,
        options: GenerationOptions = GenerationOptions.Standard
    ): Result<String> {
        val input = prompt.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        return runCatching {
            chatGateway.generate(
                messages = listOf(ChatMessage(role = ChatRole.User, content = input)),
                options = options
            ).trim()
        }.mapCatching { output ->
            require(output.isNotBlank()) { "DeepSeek returned an empty response" }
            output
        }
    }
}
