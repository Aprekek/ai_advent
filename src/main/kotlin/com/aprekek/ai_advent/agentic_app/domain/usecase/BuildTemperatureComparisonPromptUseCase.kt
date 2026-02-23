package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.TemperatureVariantResult

class BuildTemperatureComparisonPromptUseCase {
    operator fun invoke(userPrompt: String, responses: List<TemperatureVariantResult>): String {
        val responsesBlock = responses.joinToString(separator = "\n\n") { response ->
            "${response.title}:\n${response.response}"
        }
        return """
            Пользовательская задача:
            $userPrompt
            
            Ниже три ответа модели с разной температурой:
            $responsesBlock
            
            Сравни их по двум критериям:
            1) точность ответа,
            2) креативность.
            
            Дай краткую структурированную оценку по каждому варианту и сделай итоговый вывод:
            какой вариант лучший по точности, какой лучший по креативности, и какой оптимален по балансу.
        """.trimIndent()
    }
}
