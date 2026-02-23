package com.aprekek.ai_advent.agentic_app.domain.model

data class GenerationOptions(
    val maxTokens: Int? = null,
    val stopSequences: List<String> = emptyList(),
    val extraInstruction: String? = null,
    val temperature: Double? = null
) {
    companion object {
        val Standard = GenerationOptions()
    }
}
