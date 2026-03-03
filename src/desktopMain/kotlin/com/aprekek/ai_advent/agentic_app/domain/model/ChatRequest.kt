package com.aprekek.ai_advent.agentic_app.domain.model

data class ChatRequest(
    val apiKey: String,
    val messages: List<ChatMessage>,
    val model: String = DEFAULT_MODEL,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2048
) {
    companion object {
        const val DEFAULT_MODEL = "deepseek-chat"
    }
}
