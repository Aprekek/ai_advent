package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val chatId: String,
    val role: ChatRole,
    val content: String,
    val createdAt: Long
)

@Serializable
enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}
