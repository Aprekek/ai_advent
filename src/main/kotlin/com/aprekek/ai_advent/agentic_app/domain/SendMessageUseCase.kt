package com.aprekek.ai_advent.agentic_app.domain

class SendMessageUseCase(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(rawInput: String): Result<String> {
        val input = rawInput.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        return runCatching { repository.sendMessage(input).trim() }
            .mapCatching { output ->
                if (output.isBlank()) {
                    throw IllegalStateException("DeepSeek returned an empty response")
                }
                output
            }
    }
}
