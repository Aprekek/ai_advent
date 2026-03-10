package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ApiError
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.SendMessageProgress
import com.aprekek.ai_advent.agentic_app.domain.model.StreamEvent
import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatStreamingGateway
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SendMessageUseCase(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val chatStreamingGateway: ChatStreamingGateway,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) {
    fun execute(
        profileId: String,
        chatId: String,
        userInput: String
    ): Flow<SendMessageProgress> = flow {
        val normalizedInput = userInput.trim()
        require(normalizedInput.isNotEmpty()) { "Сообщение не должно быть пустым" }

        val apiKey = apiKeyRepository.getApiKey(profileId)
            ?.trim()
            .orEmpty()

        if (apiKey.isEmpty()) {
            throw ApiError.Unauthorized("Добавьте DeepSeek API key для текущего профиля")
        }

        val userMessage = ChatMessage(
            id = idGenerator.nextId(),
            chatId = chatId,
            role = ChatRole.USER,
            content = normalizedInput,
            createdAt = timeProvider.nowMillis()
        )
        chatRepository.appendMessage(profileId, chatId, userMessage)

        val history = chatRepository.listMessages(profileId, chatId)
        val profileContextMessage = buildProfileContextMessage(profileId)
        val requestMessages = profileContextMessage?.let { listOf(it) + history } ?: history
        val assistantBuffer = StringBuilder()
        var persisted = false

        suspend fun persistAssistantMessageIfNeeded() {
            if (persisted || assistantBuffer.isEmpty()) return
            val assistantMessage = ChatMessage(
                id = idGenerator.nextId(),
                chatId = chatId,
                role = ChatRole.ASSISTANT,
                content = assistantBuffer.toString(),
                createdAt = timeProvider.nowMillis()
            )
            persisted = true
            // MVP behavior: keep partially generated answer even if stream was interrupted.
            chatRepository.appendMessage(profileId, chatId, assistantMessage)
        }

        try {
            chatStreamingGateway.streamChat(
                ChatRequest(
                    apiKey = apiKey,
                    messages = requestMessages
                )
            ).collect { event ->
                when (event) {
                    StreamEvent.Started -> Unit
                    is StreamEvent.Delta -> {
                        assistantBuffer.append(event.value)
                        emit(SendMessageProgress.PartialAssistant(assistantBuffer.toString()))
                    }
                    is StreamEvent.Reconnecting -> {
                        emit(SendMessageProgress.Reconnecting(event.attempt))
                    }
                    StreamEvent.Completed -> {
                        persistAssistantMessageIfNeeded()
                        emit(SendMessageProgress.Completed)
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            persistAssistantMessageIfNeeded()
            throw cancelled
        } catch (error: Throwable) {
            persistAssistantMessageIfNeeded()
            throw error
        }
    }

    private suspend fun buildProfileContextMessage(profileId: String): ChatMessage? {
        val profile = userRepository.listProfiles().firstOrNull { it.id == profileId } ?: return null
        if (profile.descriptionItems.isEmpty()) return null

        val context = buildString {
            appendLine("User profile context:")
            appendLine("Name: ${profile.name}")
            appendLine("Description items:")
            profile.descriptionItems.forEachIndexed { index, item ->
                appendLine("${index + 1}. ${item.value}")
            }
        }.trim()

        return ChatMessage(
            id = "profile-context-$profileId",
            chatId = "",
            role = ChatRole.SYSTEM,
            content = context,
            createdAt = timeProvider.nowMillis()
        )
    }
}
