package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.port.ConversationUsageState

class ClearConversationUsageUseCase(
    private val conversationUsageState: ConversationUsageState
) {
    operator fun invoke(sessionId: String) {
        conversationUsageState.clear(sessionId)
    }
}
