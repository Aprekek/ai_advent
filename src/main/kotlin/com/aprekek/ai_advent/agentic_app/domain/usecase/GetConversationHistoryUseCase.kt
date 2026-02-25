package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState

class GetConversationHistoryUseCase(
    private val conversationState: ConversationState
) {
    operator fun invoke(sessionId: String): List<ChatMessage> = conversationState.history(sessionId)
}
