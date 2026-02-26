package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ChatGateway
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState

private const val RawSuffix = ":raw"
private const val SummarySuffix = ":summary"

class SendCompressedMessageUseCase(
    private val chatGateway: ChatGateway,
    private val conversationState: ConversationState
) {
    suspend operator fun invoke(
        sessionId: String,
        rawInput: String,
        options: GenerationOptions = GenerationOptions.Standard
    ): Result<String> {
        val input = rawInput.trim()
        if (input.isBlank()) {
            return Result.failure(IllegalArgumentException("Input must not be blank"))
        }

        val rawSessionId = rawSessionId(sessionId)
        val summarySessionId = summarySessionId(sessionId)

        val rawHistory = conversationState.history(rawSessionId)
        val summaryHistory = conversationState.history(summarySessionId)
        val userMessage = ChatMessage(role = ChatRole.User, content = input)
        val requestMessages = summaryHistory + rawHistory + userMessage

        return runCatching { chatGateway.generate(requestMessages, options).trim() }
            .mapCatching { output ->
                if (output.isBlank()) {
                    throw IllegalStateException("DeepSeek returned an empty response")
                }
                output
            }
            .onSuccess { assistantOutput ->
                archivePreviousRawToSummary(rawHistory, summarySessionId)
                saveLatestRawTurn(rawSessionId, userMessage, assistantOutput)
            }
    }

    fun compressedHistory(sessionId: String): List<ChatMessage> {
        val summaries = conversationState.history(summarySessionId(sessionId))
        val raw = conversationState.history(rawSessionId(sessionId))
        return summaries + raw
    }

    fun clearCompressedHistory(sessionId: String) {
        conversationState.clear(rawSessionId(sessionId))
        conversationState.clear(summarySessionId(sessionId))
    }

    private fun archivePreviousRawToSummary(rawHistory: List<ChatMessage>, summarySessionId: String) {
        if (rawHistory.isEmpty()) {
            return
        }

        val compressed = compressMessages(rawHistory)
        if (compressed.isNotBlank()) {
            conversationState.append(
                summarySessionId,
                ChatMessage(role = ChatRole.Assistant, content = compressed)
            )
        }
        compactSummaryIfNeeded(summarySessionId)
    }

    private fun saveLatestRawTurn(rawSessionId: String, userMessage: ChatMessage, assistantOutput: String) {
        conversationState.clear(rawSessionId)
        conversationState.append(rawSessionId, userMessage)
        conversationState.append(
            rawSessionId,
            ChatMessage(role = ChatRole.Assistant, content = assistantOutput)
        )
    }

    private fun compactSummaryIfNeeded(summarySessionId: String) {
        val summaries = conversationState.history(summarySessionId)
        if (summaries.size <= 3) {
            return
        }

        val oldestThree = summaries.take(3)
        val rest = summaries.drop(3)
        val recompressed = compressText(oldestThree.joinToString(" ") { it.content })

        conversationState.clear(summarySessionId)
        if (recompressed.isNotBlank()) {
            conversationState.append(
                summarySessionId,
                ChatMessage(role = ChatRole.Assistant, content = recompressed)
            )
        }
        rest.forEach { message ->
            conversationState.append(summarySessionId, message)
        }
    }

    private fun compressMessages(messages: List<ChatMessage>): String {
        val merged = messages.joinToString(" ") { message ->
            val role = if (message.role == ChatRole.User) "User" else "Assistant"
            "$role: ${message.content}"
        }
        return compressText(merged)
    }

    private fun compressText(source: String): String {
        val sentences = source
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (sentences.isEmpty()) {
            return ""
        }

        val selected = sentences.filterIndexed { index, _ -> (index + 1) % 5 == 0 }
        return if (selected.isNotEmpty()) {
            selected.joinToString(" ")
        } else {
            sentences.first()
        }
    }

    private fun rawSessionId(sessionId: String): String = "$sessionId$RawSuffix"

    private fun summarySessionId(sessionId: String): String = "$sessionId$SummarySuffix"
}
