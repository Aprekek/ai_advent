package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType

interface ModelExecutionGateway {
    suspend fun generate(
        provider: ProviderType,
        modelId: String,
        messages: List<ChatMessage>,
        options: GenerationOptions = GenerationOptions.Standard
    ): String
}
