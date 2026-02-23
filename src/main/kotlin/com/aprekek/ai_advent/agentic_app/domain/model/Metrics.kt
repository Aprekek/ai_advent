package com.aprekek.ai_advent.agentic_app.domain.model

data class Metrics(
    val responseTimeMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
