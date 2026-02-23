package com.aprekek.ai_advent.agentic_app.data.provider.huggingface

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekMapper
import com.aprekek.ai_advent.agentic_app.data.deepseek.ProviderRequestContext
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions

class HuggingFaceGateway(
    private val apiClient: DeepSeekApiClient,
    private val config: AppConfig
) {
    suspend fun generate(
        modelId: String,
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): String {
        val request = DeepSeekMapper.toApiRequest(
            model = modelId,
            messages = messages,
            options = options,
            responseLanguage = config.responseLanguage
        )
        val payload = apiClient.sendChatCompletion(
            request = request,
            requestContext = ProviderRequestContext(
                model = modelId,
                apiKey = config.huggingFaceApiKey,
                baseUrl = config.huggingFaceBaseUrl
            )
        )
        return payload.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }
}
