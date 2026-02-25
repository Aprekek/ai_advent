package com.aprekek.ai_advent.agentic_app.data.state

import app.cash.sqldelight.db.SqlDriver
import com.aprekek.ai_advent.agentic_app.data.state.db.SessionHistoryDatabase
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.ConversationUsage
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationState
import com.aprekek.ai_advent.agentic_app.domain.port.ConversationUsageState

class SqlDelightConversationState(
    private val driver: SqlDriver
) : ConversationState, ConversationUsageState {
    private val database = SessionHistoryDatabase(driver)
    private val queries = database.sessionHistoryQueries

    init {
        queries.createUsageTable()
    }

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

    override fun get(sessionId: String): ConversationUsage {
        val row = queries.selectUsageBySession(sessionId).executeAsOneOrNull()
        if (row == null) {
            return ConversationUsage()
        }
        return ConversationUsage(
            promptTokens = row.prompt_tokens.toInt(),
            completionTokens = row.completion_tokens.toInt(),
            totalTokens = row.total_tokens.toInt()
        )
    }

    override fun add(sessionId: String, promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        val current = get(sessionId)
        queries.upsertUsageBySession(
            session_id = sessionId,
            prompt_tokens = (current.promptTokens + promptTokens).toLong(),
            completion_tokens = (current.completionTokens + completionTokens).toLong(),
            total_tokens = (current.totalTokens + totalTokens).toLong()
        )
    }

    override fun clear(sessionId: String) {
        queries.deleteBySession(sessionId)
        queries.deleteUsageBySession(sessionId)
    }

    private fun String.toChatRole(): ChatRole = runCatching { ChatRole.valueOf(this) }
        .getOrDefault(ChatRole.User)
}
