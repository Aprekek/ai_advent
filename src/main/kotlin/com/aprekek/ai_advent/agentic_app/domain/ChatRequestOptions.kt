package com.aprekek.ai_advent.agentic_app.domain

data class ChatRequestOptions(
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val extraSystemInstruction: String? = null,
    val temperature: Double? = null,
    val modelOverride: String? = null,
    val apiKeyOverride: String? = null,
    val baseUrlOverride: String? = null
) {
    companion object {
        val Standard = ChatRequestOptions()
    }
}
