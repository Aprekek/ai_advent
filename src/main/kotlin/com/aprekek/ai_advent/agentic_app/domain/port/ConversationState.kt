package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage

interface ConversationState {
    fun history(sessionId: String): List<ChatMessage>
    fun append(sessionId: String, message: ChatMessage)
    fun trimToLast(sessionId: String, maxMessages: Int)
    fun clear(sessionId: String)
}
