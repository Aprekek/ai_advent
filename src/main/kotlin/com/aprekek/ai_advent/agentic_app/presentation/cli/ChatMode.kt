package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions

enum class ChatMode(
    val displayName: String,
    val requestOptions: GenerationOptions
) {
    Standard(
        displayName = "Standart mode",
        requestOptions = GenerationOptions.Standard
    ),
    Short(
        displayName = "Short mode",
        requestOptions = GenerationOptions(
            maxTokens = 512,
            stopSequences = listOf("\n\n"),
            extraInstruction = "Answer in one paragraph only."
        )
    ),
    Comparison(
        displayName = "Comparison mode",
        requestOptions = GenerationOptions.Standard
    ),
    TemperatureDiff(
        displayName = "Temperature diff mode",
        requestOptions = GenerationOptions.Standard
    ),
    DifferentModelsMetricsCompare(
        displayName = "Different models metrics compare",
        requestOptions = GenerationOptions.Standard
    )
}
