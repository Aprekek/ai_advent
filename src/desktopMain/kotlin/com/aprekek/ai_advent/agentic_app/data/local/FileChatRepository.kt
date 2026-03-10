package com.aprekek.ai_advent.agentic_app.data.local

import com.aprekek.ai_advent.agentic_app.domain.model.ChatContextItem
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider

class FileChatRepository(
    private val profileStateStore: ProfileStateStore,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : ChatRepository {
    override suspend fun listChats(userId: String): List<ChatThread> {
        return profileStateStore.read(userId).chats.sortedByDescending { it.updatedAt }
    }

    override suspend fun getChat(userId: String, chatId: String): ChatThread? {
        return profileStateStore.read(userId).chats.firstOrNull { it.id == chatId }
    }

    override suspend fun createChat(userId: String, title: String): ChatThread {
        val now = timeProvider.nowMillis()
        val chat = ChatThread(
            id = idGenerator.nextId(),
            userId = userId,
            title = title,
            createdAt = now,
            updatedAt = now
        )

        profileStateStore.update(userId) { state ->
            state.copy(chats = listOf(chat) + state.chats)
        }

        return chat
    }

    override suspend fun deleteChat(userId: String, chatId: String) {
        profileStateStore.update(userId) { state ->
            state.copy(
                chats = state.chats.filterNot { it.id == chatId },
                messagesByChat = state.messagesByChat - chatId
            )
        }
    }

    override suspend fun updateChatContextItems(userId: String, chatId: String, contextItems: List<String>) {
        profileStateStore.update(userId) { state ->
            val existingChat = state.chats.firstOrNull { it.id == chatId } ?: return@update state
            val normalized = contextItems
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            val existingByValue = existingChat.contextItems.associateBy { it.value }
            val updatedItems = normalized.map { value ->
                existingByValue[value] ?: ChatContextItem(
                    id = idGenerator.nextId(),
                    value = value,
                    createdAt = timeProvider.nowMillis()
                )
            }

            val updatedChat = existingChat.copy(
                contextItems = updatedItems,
                updatedAt = timeProvider.nowMillis()
            )
            state.copy(chats = listOf(updatedChat) + state.chats.filterNot { it.id == chatId })
        }
    }

    override suspend fun listMessages(userId: String, chatId: String): List<ChatMessage> {
        return profileStateStore.read(userId)
            .messagesByChat[chatId]
            .orEmpty()
            .sortedBy { it.createdAt }
    }

    override suspend fun appendMessage(userId: String, chatId: String, message: ChatMessage) {
        profileStateStore.update(userId) { state ->
            val oldMessages = state.messagesByChat[chatId].orEmpty()
            val newMessages = oldMessages + message

            val existingChat = state.chats.firstOrNull { it.id == chatId }
            val updatedChat = if (existingChat != null) {
                val shouldReplaceTitle =
                    message.role == ChatRole.USER &&
                        oldMessages.none { it.role == ChatRole.USER } &&
                        existingChat.title == "Новый чат"
                existingChat.copy(
                    title = if (shouldReplaceTitle) inferTitle(message.content) else existingChat.title,
                    updatedAt = message.createdAt
                )
            } else {
                ChatThread(
                    id = chatId,
                    userId = userId,
                    title = inferTitle(message.content),
                    createdAt = message.createdAt,
                    updatedAt = message.createdAt,
                    contextItems = emptyList()
                )
            }

            val chatsWithoutCurrent = state.chats.filterNot { it.id == chatId }

            state.copy(
                chats = listOf(updatedChat) + chatsWithoutCurrent,
                messagesByChat = state.messagesByChat + (chatId to newMessages)
            )
        }
    }

    private fun inferTitle(content: String): String {
        val compact = content
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
        if (compact.isEmpty()) return "Новый чат"
        return compact.take(48)
    }
}
