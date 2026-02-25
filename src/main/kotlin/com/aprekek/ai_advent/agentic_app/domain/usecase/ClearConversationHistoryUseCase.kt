package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState

class ClearConversationHistoryUseCase(
    private val conversationState: ConversationState
) {
    operator fun invoke(sessionId: String) {
        conversationState.clear(sessionId)
    }
}
