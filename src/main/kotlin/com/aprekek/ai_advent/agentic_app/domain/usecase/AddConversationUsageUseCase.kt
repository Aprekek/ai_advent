package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.port.ConversationUsageState

class AddConversationUsageUseCase(
    private val conversationUsageState: ConversationUsageState
) {
    operator fun invoke(sessionId: String, promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        conversationUsageState.add(sessionId, promptTokens, completionTokens, totalTokens)
    }
}
