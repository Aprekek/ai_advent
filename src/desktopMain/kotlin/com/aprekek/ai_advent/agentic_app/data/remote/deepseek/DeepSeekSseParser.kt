package com.aprekek.ai_advent.agentic_app.data.remote.deepseek

import kotlinx.serialization.json.Json

class DeepSeekSseParser(
    private val json: Json
) {
    fun parseDataPayload(payload: String): ParsedSsePayload {
        if (payload == "[DONE]") {
            return ParsedSsePayload.Done
        }

        val chunk = json.decodeFromString(DeepSeekStreamResponseChunk.serializer(), payload)
        val errorMessage = chunk.error?.message?.trim().orEmpty()
        if (errorMessage.isNotEmpty()) {
            throw IllegalStateException(errorMessage)
        }

        val content = chunk.choices.firstOrNull()?.let { choice ->
            choice.delta?.content ?: choice.message?.content
        }.orEmpty()

        return ParsedSsePayload.Delta(content)
    }
}

sealed interface ParsedSsePayload {
    data class Delta(val content: String) : ParsedSsePayload
    data object Done : ParsedSsePayload
}
