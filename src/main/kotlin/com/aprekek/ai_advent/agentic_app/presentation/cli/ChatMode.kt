package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.domain.ChatRequestOptions

enum class ChatMode(
    val displayName: String,
    val requestOptions: ChatRequestOptions
) {
    Standard(
        displayName = "Standart mode",
        requestOptions = ChatRequestOptions.Standard
    ),
    Short(
        displayName = "Short mode",
        requestOptions = ChatRequestOptions(
            maxTokens = 512,
            stopSequences = listOf("\n\n"),
            extraSystemInstruction = "Answer in one paragraph only."
        )
    ),
    Comparison(
        displayName = "Comparison mode",
        requestOptions = ChatRequestOptions.Standard
    ),
    TemperatureDiff(
        displayName = "Temperature diff mode",
        requestOptions = ChatRequestOptions.Standard
    ),
    DifferentModelsMetricsCompare(
        displayName = "Different models metrics compare",
        requestOptions = ChatRequestOptions.Standard
    )
}

enum class CostMode {
    DeepSeekReasonerPricing,
    NotAvailable
}
