package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.Metrics
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType

interface MetricsProvider {
    fun lastMetrics(provider: ProviderType): Metrics?
}
