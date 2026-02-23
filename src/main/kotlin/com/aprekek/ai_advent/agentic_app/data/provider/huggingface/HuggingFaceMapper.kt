package com.aprekek.ai_advent.agentic_app.data.provider.huggingface

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions

object HuggingFaceMapper {
    fun toApiRequest(
        model: String,
        messages: List<ChatMessage>,
        options: GenerationOptions,
        responseLanguage: String
    ): HuggingFaceChatCompletionRequest {
        val apiMessages = listOf(
            HuggingFaceMessage(
                role = "system",
                content = systemInstruction(responseLanguage, options.extraInstruction)
            )
        ) + messages.map { message ->
            HuggingFaceMessage(role = message.role.toApiRole(), content = message.content)
        }

        return HuggingFaceChatCompletionRequest(
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
        return if (trimmedExtraInstruction.isEmpty()) baseInstruction else "$baseInstruction $trimmedExtraInstruction"
    }
}
