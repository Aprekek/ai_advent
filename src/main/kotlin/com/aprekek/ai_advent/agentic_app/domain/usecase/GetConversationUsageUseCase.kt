package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ConversationUsage
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationUsageState

class GetConversationUsageUseCase(
    private val conversationUsageState: ConversationUsageState
) {
    operator fun invoke(sessionId: String): ConversationUsage = conversationUsageState.get(sessionId)
}
