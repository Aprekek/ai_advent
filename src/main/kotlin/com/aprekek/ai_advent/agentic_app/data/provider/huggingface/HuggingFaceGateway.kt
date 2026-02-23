package com.aprekek.ai_advent.agentic_app.data.provider.huggingface

import com.aprekek.ai_advent.agentic_app.data.config.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.model.ModelId

class HuggingFaceGateway(
    private val apiClient: HuggingFaceApiClient,
    private val config: AppConfig
) {
    suspend fun generate(
        modelId: ModelId,
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): String {
        val request = HuggingFaceMapper.toApiRequest(
            model = modelId.value,
            messages = messages,
            options = options,
            responseLanguage = config.responseLanguage
        )
        val payload = apiClient.sendChatCompletion(
            request = request,
            apiKey = config.huggingFaceApiKey,
            baseUrl = config.huggingFaceBaseUrl
        )
        return payload.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }
}
