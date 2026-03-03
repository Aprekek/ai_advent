package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class SelectChatUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(profileId: String, chatId: String) {
        preferencesRepository.setActiveChat(profileId, chatId)
    }
}
