package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ModelStageResult

class BuildModelComparisonPromptUseCase {
    operator fun invoke(userPrompt: String, stageOutputs: List<ModelStageResult>): String {
        val outputsBlock = stageOutputs.joinToString(separator = "\n\n") { output ->
            val metrics = output.metrics
            """
            ${output.stageTitle}
            Метрики: время=${metrics?.responseTimeMs ?: "n/a"}ms, prompt_tokens=${metrics?.promptTokens ?: "n/a"}, completion_tokens=${metrics?.completionTokens ?: "n/a"}, total_tokens=${metrics?.totalTokens ?: "n/a"}
            Ответ:
            ${output.response}
            """.trimIndent()
        }

        return """
            Пользовательский промпт:
            $userPrompt
            
            Ниже ответы и метрики трёх моделей:
            $outputsBlock
            
            Сделай краткое сравнение по критериям:
            1) качество ответа
            2) скорость
            3) ресурсоёмкость (по токенам)
            
            Верни компактный вывод: лучший по качеству, лучший по скорости, самый экономный по токенам, и общий победитель.
        """.trimIndent()
    }
}
