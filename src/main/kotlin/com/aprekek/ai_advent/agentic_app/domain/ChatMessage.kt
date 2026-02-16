package com.aprekek.ai_advent.agentic_app.domain

data class ChatMessage(
    val role: ChatRole,
    val content: String
)

enum class ChatRole {
    User,
    Assistant
}
