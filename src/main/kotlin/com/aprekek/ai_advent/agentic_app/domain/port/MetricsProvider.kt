package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.Metrics

interface MetricsProvider {
    fun lastMetrics(): Metrics?
}
