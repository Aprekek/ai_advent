package com.aprekek.ai_advent.agentic_app.domain.model

data class ChatMessage(
    val role: ChatRole,
    val content: String
)
