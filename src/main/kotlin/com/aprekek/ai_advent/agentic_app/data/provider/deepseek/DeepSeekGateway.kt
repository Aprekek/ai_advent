package com.aprekek.ai_advent.agentic_app.data.provider.deepseek

import com.aprekek.ai_advent.agentic_app.data.config.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway

class DeepSeekGateway(
    private val apiClient: DeepSeekApiClient,
    private val config: AppConfig
) : ChatGateway {
    override suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String {
        return generateWithContext(messages, options, defaultRequestContext())
    }

    suspend fun generateWithContext(
        messages: List<ChatMessage>,
        options: GenerationOptions,
        requestContext: ProviderRequestContext
    ): String {
        val request = DeepSeekMapper.toApiRequest(
            model = requestContext.model,
            messages = messages,
            options = options,
            responseLanguage = config.responseLanguage
        )
        val payload = apiClient.sendChatCompletion(
            request = request,
            requestContext = requestContext
        )
        return payload.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    suspend fun generateWithModel(
        modelId: String,
        messages: List<ChatMessage>,
        options: GenerationOptions
    ): String {
        return generateWithContext(
            messages = messages,
            options = options,
            requestContext = ProviderRequestContext(
                model = modelId,
                apiKey = config.apiKey,
                baseUrl = config.baseUrl
            )
        )
    }

    private fun defaultRequestContext(): ProviderRequestContext = ProviderRequestContext(
        model = config.model,
        apiKey = config.apiKey,
        baseUrl = config.baseUrl
    )
}
