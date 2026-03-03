package com.aprekek.ai_advent.agentic_app.data.remote.deepseek

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeepSeekStreamRequest(
    val model: String,
    val messages: List<DeepSeekMessage>,
    val stream: Boolean = true,
    val temperature: Double,
    @SerialName("max_tokens")
    val maxTokens: Int
)

@Serializable
data class DeepSeekMessage(
    val role: String,
    val content: String
)

@Serializable
data class DeepSeekStreamResponseChunk(
    val choices: List<DeepSeekChunkChoice> = emptyList(),
    val error: DeepSeekErrorBody? = null
)

@Serializable
data class DeepSeekChunkChoice(
    val delta: DeepSeekChunkDelta? = null,
    val message: DeepSeekChunkDelta? = null,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class DeepSeekChunkDelta(
    val content: String? = null
)

@Serializable
data class DeepSeekErrorEnvelope(
    val error: DeepSeekErrorBody? = null
)

@Serializable
data class DeepSeekErrorBody(
    val message: String? = null
)
