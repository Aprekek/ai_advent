package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository

class DeleteChatUseCase(
    private val chatRepository: ChatRepository
) {
    suspend fun execute(profileId: String, chatId: String) {
        chatRepository.deleteChat(profileId, chatId)
    }
}
