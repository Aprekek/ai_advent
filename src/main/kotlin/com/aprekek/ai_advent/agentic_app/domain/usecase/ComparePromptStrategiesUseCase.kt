package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.PromptComparisonResult
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway

class ComparePromptStrategiesUseCase(
    private val chatGateway: ChatGateway
) {
    suspend operator fun invoke(userPrompt: String): Result<List<PromptComparisonResult>> {
        val input = userPrompt.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        val options = GenerationOptions(maxTokens = 2048)
        return runCatching {
            val plain = requestOnce(input, options)
            val stepByStep = requestOnce("$input\n\nУсловие: реши задачу пошагово.", options)
            val generatedPrompt = requestOnce(
                "Сгенерируй промпт для решения задачи пользователя. Верни только текст промпта без пояснений.\n\nЗадача пользователя:\n$input",
                options
            )
            val generatedPromptResult = requestOnce(generatedPrompt, options)
            val experts = requestOnce(
                "$input\n\nУсловие: ответь с точки зрения трех экспертов: Аналитика, Инженера, Критика.",
                options
            )

            listOf(
                PromptComparisonResult(title = "Без доп. иструкций", response = plain),
                PromptComparisonResult(title = "Пошаговое решение", response = stepByStep),
                PromptComparisonResult(
                    title = "Генерация промпта",
                    generatedPrompt = generatedPrompt,
                    response = generatedPromptResult
                ),
                PromptComparisonResult(
                    title = "Группа экспертов (Аналитик, Инженер, Критик)",
                    response = experts
                )
            )
        }
    }

    private suspend fun requestOnce(prompt: String, options: GenerationOptions): String {
        val input = prompt.trim()
        require(input.isNotBlank()) { "Input must not be blank" }
        val output = chatGateway.generate(
            messages = listOf(ChatMessage(role = ChatRole.User, content = input)),
            options = options
        ).trim()
        require(output.isNotBlank()) { "DeepSeek returned an empty response" }
        return output
    }
}
