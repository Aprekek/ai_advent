package com.aprekek.ai_advent.agentic_app.domain.model

data class PromptComparisonResult(
    val title: String,
    val response: String,
    val generatedPrompt: String? = null
)

data class TemperatureVariantResult(
    val title: String,
    val response: String
)
