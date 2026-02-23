package com.aprekek.ai_advent.agentic_app.data.provider.deepseek

import com.aprekek.ai_advent.agentic_app.domain.model.Metrics
import com.aprekek.ai_advent.agentic_app.domain.port.MetricsProvider

class ApiClientMetricsProvider(
    private val apiClient: DeepSeekApiClient
) : MetricsProvider {
    override fun lastMetrics(): Metrics? = apiClient.lastCallMetrics?.let { metrics ->
        Metrics(
            responseTimeMs = metrics.responseTimeMs,
            promptTokens = metrics.promptTokens,
            completionTokens = metrics.completionTokens,
            totalTokens = metrics.totalTokens
        )
    }
}
