package com.aprekek.ai_advent.agentic_app.domain

class SendMessageUseCase(
    private val repository: ChatRepository,
    private val maxHistoryMessages: Int = 20
) {
    private val sessionHistory = mutableListOf<ChatMessage>()

    suspend operator fun invoke(rawInput: String): Result<String> {
        val input = rawInput.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        val userMessage = ChatMessage(role = ChatRole.User, content = input)
        val requestMessages = sessionHistory + userMessage

        return runCatching { repository.sendMessage(requestMessages).trim() }
            .mapCatching { output ->
                if (output.isBlank()) {
                    throw IllegalStateException("DeepSeek returned an empty response")
                }
                output
            }
            .onSuccess { assistantOutput ->
                sessionHistory += userMessage
                sessionHistory += ChatMessage(role = ChatRole.Assistant, content = assistantOutput)
                trimHistoryIfNeeded()
            }
    }

    private fun trimHistoryIfNeeded() {
        val overflow = sessionHistory.size - maxHistoryMessages
        if (overflow > 0) {
            repeat(overflow) {
                sessionHistory.removeAt(0)
            }
        }
    }
}
