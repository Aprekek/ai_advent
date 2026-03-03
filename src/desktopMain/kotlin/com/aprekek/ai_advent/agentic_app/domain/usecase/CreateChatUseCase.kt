package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class CreateChatUseCase(
    private val chatRepository: ChatRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(profileId: String, title: String = "Новый чат"): ChatThread {
        val chat = chatRepository.createChat(profileId, title.trim().ifBlank { "Новый чат" })
        preferencesRepository.setActiveChat(profileId, chat.id)
        return chat
    }
}
