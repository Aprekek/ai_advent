package com.aprekek.ai_advent.agentic_app.domain.model

data class ConversationUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)
