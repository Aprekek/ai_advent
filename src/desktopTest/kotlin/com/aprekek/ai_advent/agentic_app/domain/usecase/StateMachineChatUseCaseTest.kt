package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ChatContextItem
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.model.ProfileDescriptionItem
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineAction
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineDoneStatus
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineSession
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineStage
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
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class StateMachineChatUseCaseTest {
    @Test
    fun `moves planning to clarification when assistant asks for more context`() = runTest {
        val repo = InMemoryChatRepository()
        repo.createChat("u1", "FSM", ChatMode.STATE_MACHINE)
        val useCase = buildUseCase(repo) { request ->
            val system = request.messages.first().content
            if (system.contains("PLANNING stage")) {
                listOf("Уточните окружение. need context")
            } else {
                listOf("unexpected")
            }
        }

        useCase.execute("u1", "chat-1", StateMachineAction.UserInput("Сделай задачу")).toList()
        val session = repo.getChat("u1", "chat-1")?.stateMachineSession
        assertNotNull(session)
        assertEquals(StateMachineStage.CLARIFICATION, session.stage)
        assertEquals(true, session.waitingForUserInput)
    }

    @Test
    fun `completes flow to done when validation approves`() = runTest {
        val repo = InMemoryChatRepository()
        repo.createChat("u1", "FSM", ChatMode.STATE_MACHINE)
        val useCase = buildUseCase(repo) { request ->
            val system = request.messages.first().content
            when {
                system.contains("PLANNING stage") -> listOf("План реализации. full context")
                system.contains("EXECUTION stage") -> listOf("Реализовано по плану.")
                system.contains("VALIDATION stage") -> listOf("Проверка пройдена. Approve")
                else -> listOf("unknown")
            }
        }

        useCase.execute("u1", "chat-1", StateMachineAction.UserInput("Реши задачу")).toList()
        useCase.execute("u1", "chat-1", StateMachineAction.ApprovePlan).toList()
        useCase.execute("u1", "chat-1", StateMachineAction.Continue).toList()
        useCase.execute("u1", "chat-1", StateMachineAction.Continue).toList()

        val session = repo.getChat("u1", "chat-1")?.stateMachineSession
        assertNotNull(session)
        assertEquals(StateMachineStage.DONE, session.stage)
        assertEquals(StateMachineDoneStatus.DONE, session.doneStatus)
    }

    private fun buildUseCase(
        chatRepository: ChatRepository,
        responses: (ChatRequest) -> List<String>
    ): StateMachineChatUseCase {
        return StateMachineChatUseCase(
            chatRepository = chatRepository,
            userRepository = object : UserRepository {
                override suspend fun listProfiles(): List<UserProfile> = listOf(
                    UserProfile(
                        id = "u1",
                        name = "Default",
                        createdAt = 1L,
                        descriptionItems = listOf(ProfileDescriptionItem("d1", "Kotlin", 1L))
                    )
                )

                override suspend fun createProfile(name: String, descriptionItems: List<String>): UserProfile = error("N/A")
                override suspend fun updateProfile(profile: UserProfile): UserProfile = profile
                override suspend fun deleteProfile(profileId: String) = Unit
            },
            apiKeyRepository = object : ApiKeyRepository {
                override suspend fun saveApiKey(profileId: String, apiKey: String) = Unit
                override suspend fun getApiKey(profileId: String): String = "secret"
            },
            chatStreamingGateway = object : ChatStreamingGateway {
                override fun streamChat(request: ChatRequest) = flow {
                    emit(StreamEvent.Started)
                    responses(request).forEach { part -> emit(StreamEvent.Delta(part)) }
                    emit(StreamEvent.Completed)
                }
            },
            idGenerator = object : IdGenerator {
                private var c = 0
                override fun nextId(): String = "id-${c++}"
            },
            timeProvider = object : TimeProvider {
                private var now = 1L
                override fun nowMillis(): Long = now++
            }
        )
    }

    private class InMemoryChatRepository : ChatRepository {
        private val chats = mutableMapOf<String, MutableList<ChatThread>>()
        private val messages = mutableMapOf<Pair<String, String>, MutableList<ChatMessage>>()

        override suspend fun listChats(userId: String): List<ChatThread> = chats[userId].orEmpty()

        override suspend fun getChat(userId: String, chatId: String): ChatThread? {
            return chats[userId].orEmpty().firstOrNull { it.id == chatId }
        }

        override suspend fun createChat(userId: String, title: String, mode: ChatMode): ChatThread {
            val chat = ChatThread(
                id = "chat-1",
                userId = userId,
                title = title,
                createdAt = 1L,
                updatedAt = 1L,
                contextItems = listOf(ChatContextItem("c1", "Kotlin", 1L)),
                mode = mode
            )
            chats.getOrPut(userId) { mutableListOf() }.add(chat)
            return chat
        }

        override suspend fun deleteChat(userId: String, chatId: String) {
            chats[userId]?.removeAll { it.id == chatId }
            messages.remove(userId to chatId)
        }

        override suspend fun updateChatContextItems(userId: String, chatId: String, contextItems: List<String>) = Unit

        override suspend fun updateStateMachineSession(userId: String, chatId: String, session: StateMachineSession?) {
            chats[userId] = chats[userId]
                ?.map { chat -> if (chat.id == chatId) chat.copy(stateMachineSession = session) else chat }
                ?.toMutableList()
                ?: mutableListOf()
        }

        override suspend fun listMessages(userId: String, chatId: String): List<ChatMessage> {
            return messages[userId to chatId].orEmpty()
        }

        override suspend fun appendMessage(userId: String, chatId: String, message: ChatMessage) {
            messages.getOrPut(userId to chatId) { mutableListOf() }.add(message)
        }
    }
}
