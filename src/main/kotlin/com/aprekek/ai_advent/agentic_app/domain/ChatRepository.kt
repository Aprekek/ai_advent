package com.aprekek.ai_advent.agentic_app.domain

interface ChatRepository {
    suspend fun sendMessage(messages: List<ChatMessage>): String
}
