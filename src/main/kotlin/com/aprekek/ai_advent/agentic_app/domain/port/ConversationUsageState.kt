package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.ConversationUsage

interface ConversationUsageState {
    fun get(sessionId: String): ConversationUsage
    fun add(sessionId: String, promptTokens: Int, completionTokens: Int, totalTokens: Int)
    fun clear(sessionId: String)
}
