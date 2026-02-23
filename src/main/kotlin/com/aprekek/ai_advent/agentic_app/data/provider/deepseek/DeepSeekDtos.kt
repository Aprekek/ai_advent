package com.aprekek.ai_advent.agentic_app.data.provider.deepseek

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekChatCompletionRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val temperature: Double? = null
)

@Serializable
data class DeepSeekMessage(
    val role: String = "",
    val content: String = ""
)

@Serializable
data class DeepSeekChatCompletionResponse(
    val choices: List<DeepSeekChoice> = emptyList(),
    val usage: DeepSeekUsage? = null
)

@Serializable
data class DeepSeekChoice(
    val message: DeepSeekMessage = DeepSeekMessage()
)

@Serializable
data class DeepSeekErrorResponse(
    val error: DeepSeekError? = null
)

@Serializable
data class DeepSeekError(
    val message: String? = null
)

@Serializable
data class DeepSeekUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)
