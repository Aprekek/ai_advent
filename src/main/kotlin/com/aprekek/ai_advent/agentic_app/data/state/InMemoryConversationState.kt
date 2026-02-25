package com.aprekek.ai_advent.agentic_app.data.state

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ConversationUsage
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationUsageState

class InMemoryConversationState : ConversationState, ConversationUsageState {
    private val sessions: MutableMap<String, MutableList<ChatMessage>> = mutableMapOf()
    private val usage: MutableMap<String, ConversationUsage> = mutableMapOf()

    override fun history(sessionId: String): List<ChatMessage> =
        sessions[sessionId]?.toList().orEmpty()

    override fun append(sessionId: String, message: ChatMessage) {
        sessions.getOrPut(sessionId) { mutableListOf() }.add(message)
    }

    override fun trimToLast(sessionId: String, maxMessages: Int) {
        val history = sessions[sessionId] ?: return
        val overflow = history.size - maxMessages
        if (overflow <= 0) {
            return
        }
        repeat(overflow) {
            history.removeAt(0)
        }
    }

    override fun clear(sessionId: String) {
        sessions.remove(sessionId)
        usage.remove(sessionId)
    }

    override fun get(sessionId: String): ConversationUsage {
        return usage[sessionId] ?: ConversationUsage()
    }

    override fun add(sessionId: String, promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        val current = usage[sessionId] ?: ConversationUsage()
        usage[sessionId] = ConversationUsage(
            promptTokens = current.promptTokens + promptTokens,
            completionTokens = current.completionTokens + completionTokens,
            totalTokens = current.totalTokens + totalTokens
        )
    }
}
