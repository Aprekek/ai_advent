package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.ProfileWorkspace
import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class LoadWorkspaceUseCase(
    private val chatRepository: ChatRepository,
    private val preferencesRepository: PreferencesRepository,
    private val apiKeyRepository: ApiKeyRepository
) {
    suspend fun execute(profileId: String): ProfileWorkspace {
        val chats = chatRepository.listChats(profileId)
        val preferences = preferencesRepository.load()

        val preferredChatId = preferences.activeChatByProfile[profileId]
        val selectedChatId = when {
            preferredChatId != null && chats.any { it.id == preferredChatId } -> preferredChatId
            chats.isNotEmpty() -> chats.first().id
            else -> null
        }

        if (selectedChatId != null && selectedChatId != preferredChatId) {
            preferencesRepository.setActiveChat(profileId, selectedChatId)
        }

        val messages = selectedChatId
            ?.let { chatRepository.listMessages(profileId, it) }
            .orEmpty()
        val selectedChat = chats.firstOrNull { it.id == selectedChatId }

        val hasApiKey = !apiKeyRepository.getApiKey(profileId).isNullOrBlank()

        return ProfileWorkspace(
            chats = chats,
            selectedChatId = selectedChatId,
            selectedChatMode = selectedChat?.mode ?: ChatMode.STANDARD,
            stateMachineSession = selectedChat?.stateMachineSession,
            messages = messages,
            hasApiKey = hasApiKey
        )
    }
}
