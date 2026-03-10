package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread

interface ChatRepository {
    suspend fun listChats(userId: String): List<ChatThread>
    suspend fun getChat(userId: String, chatId: String): ChatThread?
    suspend fun createChat(userId: String, title: String): ChatThread
    suspend fun deleteChat(userId: String, chatId: String)
    suspend fun updateChatContextItems(userId: String, chatId: String, contextItems: List<String>)
    suspend fun listMessages(userId: String, chatId: String): List<ChatMessage>
    suspend fun appendMessage(userId: String, chatId: String, message: ChatMessage)
}
