package com.aprekek.ai_advent.agentic_app.data.provider.deepseek

import com.aprekek.ai_advent.agentic_app.data.provider.huggingface.HuggingFaceApiClient
import com.aprekek.ai_advent.agentic_app.domain.model.Metrics
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.port.MetricsProvider

class ApiClientMetricsProvider(
    private val deepSeekApiClient: DeepSeekApiClient,
    private val huggingFaceApiClient: HuggingFaceApiClient
) : MetricsProvider {
    override fun lastMetrics(provider: ProviderType): Metrics? = when (provider) {
        ProviderType.DeepSeek -> deepSeekApiClient.lastCallMetrics?.toDomain()
        ProviderType.HuggingFace -> huggingFaceApiClient.lastCallMetrics?.toDomain()
    }

    private fun CallMetrics.toDomain(): Metrics = Metrics(
        responseTimeMs = responseTimeMs,
        promptTokens = promptTokens,
        completionTokens = completionTokens,
        totalTokens = totalTokens
    )

    private fun com.aprekek.ai_advent.agentic_app.data.provider.huggingface.HuggingFaceCallMetrics.toDomain(): Metrics =
        Metrics(
            responseTimeMs = responseTimeMs,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens
        )
}
