package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class CreateChatUseCase(
    private val chatRepository: ChatRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(
        profileId: String,
        title: String = "Новый чат",
        mode: ChatMode = ChatMode.STANDARD
    ): ChatThread {
        val chat = chatRepository.createChat(
            userId = profileId,
            title = title.trim().ifBlank { "Новый чат" },
            mode = mode
        )
        preferencesRepository.setActiveChat(profileId, chat.id)
        return chat
    }
}
