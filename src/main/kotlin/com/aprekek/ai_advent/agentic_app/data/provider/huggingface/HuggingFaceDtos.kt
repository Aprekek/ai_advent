package com.aprekek.ai_advent.agentic_app.data.provider.huggingface

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HuggingFaceChatCompletionRequest(
    val model: String,
    val messages: List<HuggingFaceMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stop: List<String>? = null,
    val temperature: Double? = null
)

@Serializable
data class HuggingFaceMessage(
    val role: String = "",
    val content: String = ""
)

@Serializable
data class HuggingFaceChatCompletionResponse(
    val choices: List<HuggingFaceChoice> = emptyList(),
    val usage: HuggingFaceUsage? = null
)

@Serializable
data class HuggingFaceChoice(
    val message: HuggingFaceMessage = HuggingFaceMessage()
)

@Serializable
data class HuggingFaceUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int? = null,
    @SerialName("completion_tokens")
    val completionTokens: Int? = null,
    @SerialName("total_tokens")
    val totalTokens: Int? = null
)
