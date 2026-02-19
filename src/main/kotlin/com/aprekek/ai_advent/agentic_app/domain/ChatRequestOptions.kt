package com.aprekek.ai_advent.agentic_app.domain

data class ChatRequestOptions(
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val extraSystemInstruction: String? = null,
    val temperature: Double? = null
) {
    companion object {
        val Standard = ChatRequestOptions()
    }
}
