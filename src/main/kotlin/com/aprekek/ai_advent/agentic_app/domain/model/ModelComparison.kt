package com.aprekek.ai_advent.agentic_app.domain.model

enum class ProviderType {
    DeepSeek,
    HuggingFace
}

enum class PricingMode {
    DeepSeekReasonerPricing,
    NotAvailable
}

data class ModelVariant(
    val title: String,
    val provider: ProviderType,
    val modelId: String,
    val pricingMode: PricingMode
)

data class ModelStageResult(
    val stageTitle: String,
    val response: String,
    val metrics: Metrics?,
    val pricingMode: PricingMode
)
