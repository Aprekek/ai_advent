package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions

object DeepSeekMapper {
    fun toApiRequest(
        model: String,
        messages: List<ChatMessage>,
        options: GenerationOptions,
        responseLanguage: String
    ): DeepSeekChatCompletionRequest {
        val apiMessages = listOf(
            DeepSeekMessage(
                role = "system",
                content = systemInstruction(responseLanguage, options.extraInstruction)
            )
        ) + messages.map { message ->
            DeepSeekMessage(
                role = message.role.toApiRole(),
                content = message.content
            )
        }

        return DeepSeekChatCompletionRequest(
            model = model,
            messages = apiMessages,
            maxTokens = options.maxTokens,
            stop = options.stopSequences.takeIf { it.isNotEmpty() },
            temperature = options.temperature
        )
    }

    private fun ChatRole.toApiRole(): String = when (this) {
        ChatRole.User -> "user"
        ChatRole.Assistant -> "assistant"
    }

    private fun systemInstruction(language: String, extraInstruction: String?): String {
        val baseInstruction = "Always respond in $language unless the user explicitly requests another language."
        val trimmedExtraInstruction = extraInstruction?.trim().orEmpty()
        return if (trimmedExtraInstruction.isEmpty()) {
            baseInstruction
        } else {
            "$baseInstruction $trimmedExtraInstruction"
        }
    }
}
