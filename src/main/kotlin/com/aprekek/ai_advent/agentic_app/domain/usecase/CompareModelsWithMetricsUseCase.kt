package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.ModelStageResult
import com.aprekek.ai_advent.agentic_app.domain.model.ModelVariant
import com.aprekek.ai_advent.agentic_app.domain.port.MetricsProvider
import com.aprekek.ai_advent.agentic_app.domain.port.ModelExecutionGateway

class CompareModelsWithMetricsUseCase(
    private val modelExecutionGateway: ModelExecutionGateway,
    private val metricsProvider: MetricsProvider
) {
    suspend operator fun invoke(
        userPrompt: String,
        stages: List<ModelVariant>
    ): Result<List<ModelStageResult>> {
        val prompt = userPrompt.trim()
        if (prompt.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        val messages = listOf(ChatMessage(role = ChatRole.User, content = prompt))
        return runCatching {
            stages.map { stage ->
                val response = modelExecutionGateway.generate(
                    provider = stage.provider,
                    modelId = stage.modelId,
                    messages = messages,
                    options = GenerationOptions.Standard
                )
                ModelStageResult(
                    stageTitle = stage.title,
                    response = response,
                    metrics = metricsProvider.lastMetrics(stage.provider),
                    pricingMode = stage.pricingMode
                )
            }
        }
    }
}
