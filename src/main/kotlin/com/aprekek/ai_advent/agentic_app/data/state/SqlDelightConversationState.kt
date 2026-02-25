package com.aprekek.ai_advent.agentic_app.data.state

import app.cash.sqldelight.db.SqlDriver
import com.aprekek.ai_advent.agentic_app.data.state.db.SessionHistoryDatabase
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState

class SqlDelightConversationState(
    private val driver: SqlDriver
) : ConversationState {
    private val database = SessionHistoryDatabase(driver)
    private val queries = database.sessionHistoryQueries

    override fun history(sessionId: String): List<ChatMessage> {
        return queries.selectBySession(sessionId).executeAsList().map { row ->
            ChatMessage(
                role = row.role.toChatRole(),
                content = row.content
            )
        }
    }

    override fun append(sessionId: String, message: ChatMessage) {
        queries.insertMessage(
            session_id = sessionId,
            role = message.role.name,
            content = message.content,
            created_at = System.currentTimeMillis()
        )
    }

    override fun trimToLast(sessionId: String, maxMessages: Int) {
        val rows = queries.selectBySession(sessionId).executeAsList()
        val overflow = rows.size - maxMessages
        if (overflow <= 0) {
            return
        }

        rows.take(overflow).forEach { row ->
            queries.deleteById(row.id)
        }
    }

    override fun clear(sessionId: String) {
        queries.deleteBySession(sessionId)
    }

    private fun String.toChatRole(): ChatRole = runCatching { ChatRole.valueOf(this) }
        .getOrDefault(ChatRole.User)
}
