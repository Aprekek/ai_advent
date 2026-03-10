package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.model.ProfileDescriptionItem
import com.aprekek.ai_advent.agentic_app.domain.model.SendMessageProgress
import com.aprekek.ai_advent.agentic_app.domain.model.StreamEvent
import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile
import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatStreamingGateway
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class SendMessageUseCaseTest {
    @Test
    fun `stores user and assistant messages while emitting partial updates`() = runTest {
        val chatRepository = InMemoryChatRepository()
        chatRepository.createChat("u1", "Новый чат")
        var capturedRequest: ChatRequest? = null
        val useCase = SendMessageUseCase(
            chatRepository = chatRepository,
            userRepository = object : UserRepository {
                override suspend fun listProfiles(): List<UserProfile> = listOf(
                    UserProfile(
                        id = "u1",
                        name = "Default",
                        createdAt = 1L,
                        descriptionItems = listOf(
                            ProfileDescriptionItem("d1", "loves concise answers", 1L)
                        )
                    )
                )

                override suspend fun createProfile(name: String, descriptionItems: List<String>): UserProfile {
                    error("not needed")
                }

                override suspend fun updateProfile(profile: UserProfile): UserProfile = profile

                override suspend fun deleteProfile(profileId: String) = Unit
            },
            apiKeyRepository = object : ApiKeyRepository {
                override suspend fun saveApiKey(profileId: String, apiKey: String) = Unit
                override suspend fun getApiKey(profileId: String): String = "secret"
            },
            chatStreamingGateway = object : ChatStreamingGateway {
                override fun streamChat(request: ChatRequest) = flow {
                    capturedRequest = request
                    emit(StreamEvent.Started)
                    emit(StreamEvent.Delta("Hel"))
                    emit(StreamEvent.Delta("lo"))
                    emit(StreamEvent.Completed)
                }
            },
            idGenerator = object : IdGenerator {
                private var counter = 0
                override fun nextId(): String = "id-${counter++}"
            },
            timeProvider = object : TimeProvider {
                override fun nowMillis(): Long = 1L
            }
        )

        val progress = useCase.execute("u1", "chat-1", "Hi").toList()
        val persisted = chatRepository.listMessages("u1", "chat-1")

        assertEquals(
            listOf(
                SendMessageProgress.PartialAssistant("Hel"),
                SendMessageProgress.PartialAssistant("Hello"),
                SendMessageProgress.Completed
            ),
            progress
        )
        assertEquals(2, persisted.size)
        assertEquals(ChatRole.USER, persisted[0].role)
        assertEquals("Hi", persisted[0].content)
        assertEquals(ChatRole.ASSISTANT, persisted[1].role)
        assertEquals("Hello", persisted[1].content)
        assertEquals(ChatRole.SYSTEM, capturedRequest?.messages?.first()?.role)
        assertEquals(true, capturedRequest?.messages?.first()?.content?.contains("loves concise answers"))
    }

    private class InMemoryChatRepository : ChatRepository {
        private val chats = mutableMapOf<String, MutableList<ChatThread>>()
        private val messages = mutableMapOf<Pair<String, String>, MutableList<ChatMessage>>()

        override suspend fun listChats(userId: String): List<ChatThread> {
            return chats[userId].orEmpty()
        }

        override suspend fun createChat(userId: String, title: String): ChatThread {
            val chat = ChatThread(
                id = "chat-1",
                userId = userId,
                title = title,
                createdAt = 1,
                updatedAt = 1
            )
            chats.getOrPut(userId) { mutableListOf() }.add(chat)
            return chat
        }

        override suspend fun deleteChat(userId: String, chatId: String) {
            chats[userId]?.removeAll { it.id == chatId }
            messages.remove(userId to chatId)
        }

        override suspend fun listMessages(userId: String, chatId: String): List<ChatMessage> {
            return messages[userId to chatId].orEmpty()
        }

        override suspend fun appendMessage(userId: String, chatId: String, message: ChatMessage) {
            messages.getOrPut(userId to chatId) { mutableListOf() }.add(message)
        }
    }
}
