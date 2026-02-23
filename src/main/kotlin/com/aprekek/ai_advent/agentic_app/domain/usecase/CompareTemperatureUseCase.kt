package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.TemperatureVariantResult
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway

class CompareTemperatureUseCase(
    private val chatGateway: ChatGateway
) {
    suspend operator fun invoke(userPrompt: String): Result<List<TemperatureVariantResult>> {
        val input = userPrompt.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        return runCatching {
            listOf(
                TemperatureVariantResult(
                    title = "Температура: 0 (точность)",
                    response = requestOnce(input, GenerationOptions(temperature = 0.0))
                ),
                TemperatureVariantResult(
                    title = "Температура: 1 (баланс)",
                    response = requestOnce(input, GenerationOptions(temperature = 1.0))
                ),
                TemperatureVariantResult(
                    title = "Температура: 2 (креатив)",
                    response = requestOnce(input, GenerationOptions(temperature = 2.0))
                )
            )
        }
    }

    private suspend fun requestOnce(prompt: String, options: GenerationOptions): String {
        val output = chatGateway.generate(
            messages = listOf(ChatMessage(role = ChatRole.User, content = prompt)),
            options = options
        ).trim()
        require(output.isNotBlank()) { "DeepSeek returned an empty response" }
        return output
    }
}
