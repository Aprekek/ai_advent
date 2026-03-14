package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ApiError
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineAction
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineDoneStatus
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineProgress
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineSession
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineStage
import com.aprekek.ai_advent.agentic_app.domain.model.StreamEvent
import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatRepository
import com.aprekek.ai_advent.agentic_app.domain.port.ChatStreamingGateway
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StateMachineChatUseCase(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val chatStreamingGateway: ChatStreamingGateway,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) {
    private val actionMutex = Mutex()

    fun execute(
        profileId: String,
        chatId: String,
        action: StateMachineAction
    ): Flow<StateMachineProgress> = flow {
        actionMutex.withLock {
        val chat = chatRepository.getChat(profileId, chatId)
            ?: throw IllegalStateException("Чат не найден")
        require(chat.mode == ChatMode.STATE_MACHINE) { "FSM use case поддерживает только STATE_MACHINE чаты" }

        val apiKey = apiKeyRepository.getApiKey(profileId)
            ?.trim()
            .orEmpty()
        if (apiKey.isEmpty()) {
            throw ApiError.Unauthorized("Добавьте DeepSeek API key")
        }

        val currentSession = chat.stateMachineSession
        when (action) {
            StateMachineAction.Stop -> {
                val session = currentSession
                if (session == null || session.stage == StateMachineStage.DONE) {
                    emit(StateMachineProgress.Completed)
                    return@flow
                }
                val canceled = session.copy(
                    stage = StateMachineStage.DONE,
                    doneStatus = StateMachineDoneStatus.CANCELED,
                    waitingForUserInput = false,
                    lastFailureReason = null
                )
                saveSession(profileId, chatId, session, canceled)
                appendAssistant(profileId, chatId, "Canceled")
                emit(StateMachineProgress.SessionUpdated(canceled))
                emit(StateMachineProgress.Completed)
            }

            is StateMachineAction.UserInput -> {
                val normalized = action.value.trim()
                require(normalized.isNotEmpty()) { "Сообщение не должно быть пустым" }

                val session = ensureSession(currentSession, taskSeed = normalized)
                when (session.stage) {
                    StateMachineStage.PLANNING, StateMachineStage.CLARIFICATION -> {
                        val invariantViolation = detectInvariantViolation(profileId, chat, normalized)
                        if (invariantViolation != null) {
                            appendUser(profileId, chatId, normalized)
                            appendAssistant(profileId, chatId, invariantViolation)
                            val updated = session.copy(waitingForUserInput = true)
                            saveSession(profileId, chatId, session, updated)
                            emit(StateMachineProgress.SessionUpdated(updated))
                            emit(StateMachineProgress.Completed)
                            return@flow
                        }

                        appendUser(profileId, chatId, normalized)
                        val inputSession = if (session.task.isBlank()) session.copy(task = normalized) else session
                        saveSession(profileId, chatId, session, inputSession)
                        emit(StateMachineProgress.SessionUpdated(inputSession.copy(waitingForUserInput = false)))

                        val assistantText = streamAssistant(
                            profileId = profileId,
                            chat = chat,
                            session = inputSession,
                            apiKey = apiKey,
                            stage = inputSession.stage,
                            emitProgress = { emit(it) }
                        )

                        val cleanText = stripControlKeywords(assistantText).trim()
                        val fullContext = containsFullContext(assistantText)

                        val nextSession = if (fullContext) {
                            inputSession.copy(
                                stage = StateMachineStage.PLANNING,
                                waitingForUserInput = true,
                                planDraft = cleanText,
                                hasFullContext = true,
                                doneStatus = null
                            )
                        } else {
                            inputSession.copy(
                                stage = StateMachineStage.CLARIFICATION,
                                waitingForUserInput = true,
                                planDraft = cleanText,
                                hasFullContext = false,
                                doneStatus = null
                            )
                        }

                        saveSession(profileId, chatId, inputSession, nextSession)
                        emit(StateMachineProgress.SessionUpdated(nextSession))
                        if (fullContext) {
                            appendAssistant(profileId, chatId, "Перейти к выполнению плана?")
                        }
                        emit(StateMachineProgress.Completed)
                    }

                    StateMachineStage.DONE -> {
                        // Start a new FSM cycle after completion.
                        val restarted = freshSession(normalized)
                        saveSession(profileId, chatId, session, restarted)
                        emit(StateMachineProgress.SessionUpdated(restarted))
                        appendUser(profileId, chatId, normalized)

                        val assistantText = streamAssistant(
                            profileId = profileId,
                            chat = chat,
                            session = restarted,
                            apiKey = apiKey,
                            stage = StateMachineStage.PLANNING,
                            emitProgress = { emit(it) }
                        )
                        val cleanText = stripControlKeywords(assistantText).trim()
                        val fullContext = containsFullContext(assistantText)
                        val nextSession = if (fullContext) {
                            restarted.copy(
                                stage = StateMachineStage.PLANNING,
                                waitingForUserInput = true,
                                planDraft = cleanText,
                                hasFullContext = true
                            )
                        } else {
                            restarted.copy(
                                stage = StateMachineStage.CLARIFICATION,
                                waitingForUserInput = true,
                                planDraft = cleanText,
                                hasFullContext = false
                            )
                        }
                        saveSession(profileId, chatId, restarted, nextSession)
                        emit(StateMachineProgress.SessionUpdated(nextSession))
                        if (fullContext) {
                            appendAssistant(profileId, chatId, "Перейти к выполнению плана?")
                        }
                        emit(StateMachineProgress.Completed)
                    }

                    else -> {
                        throw IllegalStateException("Ввод пользователя сейчас не требуется")
                    }
                }
            }

            StateMachineAction.ApprovePlan -> {
                val session = requireSession(currentSession)
                require(session.stage == StateMachineStage.PLANNING || session.stage == StateMachineStage.CLARIFICATION) {
                    "Подтверждение плана доступно только на стадии планирования"
                }
                require(session.planDraft.isNotBlank()) { "Нет сформированного плана" }
                require(session.hasFullContext) { "Контекст неполный. Используйте пропуск уточнений или уточните задачу." }
                val updated = session.copy(
                    stage = StateMachineStage.EXECUTION,
                    waitingForUserInput = false,
                    approvedPlan = session.planDraft
                )
                saveSession(profileId, chatId, session, updated)
                emit(StateMachineProgress.SessionUpdated(updated))
                emit(StateMachineProgress.Completed)
            }

            StateMachineAction.SkipClarificationToExecution -> {
                val session = requireSession(currentSession)
                require(session.stage == StateMachineStage.PLANNING || session.stage == StateMachineStage.CLARIFICATION) {
                    "Пропуск уточнений доступен только на стадии planning/clarification"
                }
                require(session.planDraft.isNotBlank()) { "Нет плана для выполнения" }
                val updated = session.copy(
                    stage = StateMachineStage.EXECUTION,
                    waitingForUserInput = false,
                    approvedPlan = session.planDraft
                )
                saveSession(profileId, chatId, session, updated)
                emit(StateMachineProgress.SessionUpdated(updated))
                emit(StateMachineProgress.Completed)
            }

            StateMachineAction.Continue -> {
                val session = requireSession(currentSession)
                when (session.stage) {
                    StateMachineStage.EXECUTION -> {
                        require(!session.waitingForUserInput) { "Стадия ожидает подтверждения пользователя" }
                        val text = streamAssistant(
                            profileId = profileId,
                            chat = chat,
                            session = session,
                            apiKey = apiKey,
                            stage = StateMachineStage.EXECUTION,
                            emitProgress = { emit(it) }
                        )
                        val updated = session.copy(
                            stage = StateMachineStage.VALIDATION,
                            waitingForUserInput = false,
                            executionResult = text.trim()
                        )
                        saveSession(profileId, chatId, session, updated)
                        emit(StateMachineProgress.SessionUpdated(updated))
                        emit(StateMachineProgress.Completed)
                    }

                    StateMachineStage.VALIDATION -> {
                        require(!session.waitingForUserInput) { "Стадия ожидает выбора пользователя" }
                        val text = streamAssistant(
                            profileId = profileId,
                            chat = chat,
                            session = session,
                            apiKey = apiKey,
                            stage = StateMachineStage.VALIDATION,
                            emitProgress = { emit(it) }
                        )

                        val approve = containsApprove(text)
                        val decline = containsDecline(text)
                        val updated = if (approve && !decline) {
                            session.copy(
                                stage = StateMachineStage.DONE,
                                doneStatus = StateMachineDoneStatus.DONE,
                                waitingForUserInput = false,
                                validationResult = text.trim()
                            )
                        } else {
                            session.copy(
                                stage = StateMachineStage.VALIDATION,
                                waitingForUserInput = true,
                                validationResult = text.trim()
                            )
                        }
                        saveSession(profileId, chatId, session, updated)
                        if (updated.stage == StateMachineStage.DONE && updated.doneStatus == StateMachineDoneStatus.DONE) {
                            appendAssistant(profileId, chatId, "Done")
                            val restarted = freshSession(taskSeed = "")
                            saveSession(profileId, chatId, updated, restarted)
                            emit(StateMachineProgress.SessionUpdated(restarted))
                            emit(StateMachineProgress.Completed)
                            return@flow
                        }
                        emit(StateMachineProgress.SessionUpdated(updated))
                        emit(StateMachineProgress.Completed)
                    }

                    else -> throw IllegalStateException("Continue недоступен для текущей стадии")
                }
            }

            StateMachineAction.ValidationRework -> {
                val session = requireSession(currentSession)
                require(session.stage == StateMachineStage.VALIDATION && session.waitingForUserInput) {
                    "Повтор execution доступен только после decline на validation"
                }
                val updated = session.copy(
                    stage = StateMachineStage.EXECUTION,
                    waitingForUserInput = false
                )
                saveSession(profileId, chatId, session, updated)
                emit(StateMachineProgress.SessionUpdated(updated))
                emit(StateMachineProgress.Completed)
            }

            StateMachineAction.ValidationAcceptCurrent -> {
                val session = requireSession(currentSession)
                require(session.stage == StateMachineStage.VALIDATION && session.waitingForUserInput) {
                    "Принятие решения доступно только после validation"
                }
                val updated = session.copy(
                    stage = StateMachineStage.DONE,
                    doneStatus = StateMachineDoneStatus.DONE,
                    waitingForUserInput = false
                )
                saveSession(profileId, chatId, session, updated)
                appendAssistant(profileId, chatId, "Done")
                val restarted = freshSession(taskSeed = "")
                saveSession(profileId, chatId, updated, restarted)
                emit(StateMachineProgress.SessionUpdated(restarted))
                emit(StateMachineProgress.Completed)
            }
        }
        }
    }

    private suspend fun streamAssistant(
        profileId: String,
        chat: ChatThread,
        session: StateMachineSession,
        apiKey: String,
        stage: StateMachineStage,
        emitProgress: suspend (StateMachineProgress) -> Unit
    ): String {
        val requestMessages = buildRequestMessages(profileId, chat, session, stage)
        val rawBuffer = StringBuilder()
        var persisted = false

        suspend fun persistIfNeeded() {
            if (persisted || rawBuffer.isEmpty()) return
            persisted = true
            val clean = stripControlKeywords(rawBuffer.toString()).trim()
            if (clean.isNotEmpty()) {
                appendAssistant(profileId, chat.id, clean)
            }
        }

        try {
            chatStreamingGateway.streamChat(
                ChatRequest(
                    apiKey = apiKey,
                    messages = requestMessages
                )
            ).collect { event ->
                when (event) {
                    StreamEvent.Started -> Unit
                    is StreamEvent.Delta -> {
                        rawBuffer.append(event.value)
                        emitProgress(StateMachineProgress.PartialAssistant(stripControlKeywords(rawBuffer.toString())))
                    }
                    is StreamEvent.Reconnecting -> emitProgress(StateMachineProgress.Reconnecting(event.attempt))
                    StreamEvent.Completed -> {
                        persistIfNeeded()
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            persistIfNeeded()
            throw cancelled
        } catch (error: Throwable) {
            persistIfNeeded()
            val failed = session.copy(
                stage = StateMachineStage.DONE,
                doneStatus = StateMachineDoneStatus.FAILED,
                waitingForUserInput = false,
                lastFailureReason = error.message ?: "Не удалось выполнить стадию"
            )
            saveSession(profileId, chat.id, session, failed)
            emitProgress(StateMachineProgress.SessionUpdated(failed))
            appendAssistant(profileId, chat.id, "Failed: ${failed.lastFailureReason}")
            throw error
        }

        return rawBuffer.toString()
    }

    private suspend fun buildRequestMessages(
        profileId: String,
        chat: ChatThread,
        session: StateMachineSession,
        stage: StateMachineStage
    ): List<ChatMessage> {
        val now = timeProvider.nowMillis()
        val profile = userRepository.listProfiles().firstOrNull { it.id == profileId }
        val profileInvariants = profile?.descriptionItems.orEmpty().map { it.value }
        val chatInvariants = chat.contextItems.map { it.value }
        val invariantText = (profileInvariants + chatInvariants)
            .filter { it.isNotBlank() }
            .joinToString(separator = "\n- ", prefix = "- ")

        val stagePrompt = when (stage) {
            StateMachineStage.PLANNING, StateMachineStage.CLARIFICATION -> """
                You are in PLANNING stage.
                Build or refine implementation plan only. Do not produce final code solution.
                If context from user is missing, ask specific questions and end your message with: need context
                If context is enough, provide final plan and end your message with: full context
            """.trimIndent()

            StateMachineStage.EXECUTION -> """
                You are in EXECUTION stage.
                Follow ONLY approved plan below. Provide implementation/answer.
                The approved plan and execution context are provided in the USER message.
            """.trimIndent()

            StateMachineStage.VALIDATION -> """
                You are in VALIDATION stage.
                Validate execution result against approved plan.
                Approved plan:
                ${session.approvedPlan}

                Execution result:
                ${session.executionResult}

                End with Approve if matches, or Decline if deviations exist.
            """.trimIndent()

            StateMachineStage.DONE -> "DONE stage"
        }

        val invariantPrompt = if (invariantText.isBlank()) {
            ""
        } else {
            """
            Invariants (must not be violated):
            $invariantText
            If user request conflicts with invariants, decline request and explain conflict.
            """.trimIndent()
        }

        val system = ChatMessage(
            id = "fsm-system-${stage.name.lowercase()}",
            chatId = chat.id,
            role = ChatRole.SYSTEM,
            content = listOf(stagePrompt, invariantPrompt).filter { it.isNotBlank() }.joinToString("\n\n"),
            createdAt = now
        )

        return when (stage) {
            StateMachineStage.EXECUTION -> listOf(
                system,
                ChatMessage(
                    id = "fsm-task",
                    chatId = chat.id,
                    role = ChatRole.USER,
                    content = executionUserPrompt(session),
                    createdAt = now
                )
            )

            StateMachineStage.VALIDATION -> listOf(system)
            else -> {
                val sessionHistory = chatRepository.listMessages(profileId, chat.id)
                    .filter { it.createdAt >= session.sessionStartedAt }
                    .filterNot(::isUiOnlyMessage)
                listOf(system) + sessionHistory
            }
        }
    }

    private fun isUiOnlyMessage(message: ChatMessage): Boolean {
        val text = message.content.trim()
        if (text.isEmpty()) return true
        if (text == "Перейти к выполнению плана?") return true
        if (text == "Done" || text == "Canceled") return true
        if (text.startsWith("Failed:")) return true
        if (text.startsWith("Transition:")) return true
        return false
    }

    private fun executionUserPrompt(session: StateMachineSession): String {
        val approvedPlan = session.approvedPlan.trim()
        val execution = session.executionResult.trim()
        val validation = session.validationResult.trim()

        return if (execution.isNotEmpty() && validation.isNotEmpty()) {
            buildString {
                appendLine("Approved plan from Planning:")
                appendLine(approvedPlan)
                appendLine()
                appendLine("Previous Execution result:")
                appendLine(execution)
                appendLine()
                appendLine("Validation feedback:")
                append(validation)
            }.trim()
        } else {
            approvedPlan
        }
    }

    private suspend fun appendUser(profileId: String, chatId: String, text: String) {
        chatRepository.appendMessage(
            userId = profileId,
            chatId = chatId,
            message = ChatMessage(
                id = idGenerator.nextId(),
                chatId = chatId,
                role = ChatRole.USER,
                content = text,
                createdAt = timeProvider.nowMillis()
            )
        )
    }

    private suspend fun appendAssistant(profileId: String, chatId: String, text: String) {
        chatRepository.appendMessage(
            userId = profileId,
            chatId = chatId,
            message = ChatMessage(
                id = idGenerator.nextId(),
                chatId = chatId,
                role = ChatRole.ASSISTANT,
                content = text,
                createdAt = timeProvider.nowMillis()
            )
        )
    }

    private suspend fun appendSystem(profileId: String, chatId: String, text: String) {
        chatRepository.appendMessage(
            userId = profileId,
            chatId = chatId,
            message = ChatMessage(
                id = idGenerator.nextId(),
                chatId = chatId,
                role = ChatRole.SYSTEM,
                content = text,
                createdAt = timeProvider.nowMillis()
            )
        )
    }

    private suspend fun saveSession(
        profileId: String,
        chatId: String,
        previousSession: StateMachineSession?,
        session: StateMachineSession
    ) {
        val previousMainStage = previousSession?.stage?.toMainStageName()
        val nextMainStage = session.stage.toMainStageName()
        if (previousMainStage != null && previousMainStage != nextMainStage) {
            appendSystem(profileId, chatId, "Transition: $previousMainStage -> $nextMainStage")
        }
        chatRepository.updateStateMachineSession(profileId, chatId, session)
    }

    private fun StateMachineStage.toMainStageName(): String {
        return when (this) {
            StateMachineStage.PLANNING, StateMachineStage.CLARIFICATION -> "Planning"
            StateMachineStage.EXECUTION -> "Execution"
            StateMachineStage.VALIDATION -> "Validation"
            StateMachineStage.DONE -> "Done"
        }
    }

    private fun requireSession(session: StateMachineSession?): StateMachineSession {
        return session ?: throw IllegalStateException("FSM сессия не инициализирована")
    }

    private fun ensureSession(session: StateMachineSession?, taskSeed: String): StateMachineSession {
        if (session == null || session.stage == StateMachineStage.DONE) {
            return freshSession(taskSeed)
        }
        return session
    }

    private fun freshSession(taskSeed: String): StateMachineSession {
        return StateMachineSession(
            sessionId = idGenerator.nextId(),
            stage = StateMachineStage.PLANNING,
            waitingForUserInput = true,
            task = taskSeed,
            sessionStartedAt = timeProvider.nowMillis(),
            doneStatus = null,
            planDraft = "",
            hasFullContext = false,
            approvedPlan = "",
            executionResult = "",
            validationResult = "",
            lastFailureReason = null
        )
    }

    private suspend fun detectInvariantViolation(
        profileId: String,
        chat: ChatThread,
        userInput: String
    ): String? {
        val profile = userRepository.listProfiles().firstOrNull { it.id == profileId }
        val invariantText = buildString {
            profile?.descriptionItems?.forEach { appendLine(it.value) }
            chat.contextItems.forEach { appendLine(it.value) }
        }.lowercase()
        if (invariantText.isBlank()) return null

        val languages = listOf("kotlin", "c++", "cpp", "java", "python", "javascript", "typescript", "go", "rust")
        val required = languages.filter { invariantText.contains(it) }.toSet()
        if (required.isEmpty()) return null

        val normalizedInput = userInput.lowercase()
        val requested = languages.filter { normalizedInput.contains(it) }.toSet()
        val conflict = requested.firstOrNull { it !in required }
        if (conflict == null) return null

        return "Запрос отклонен: нарушен инвариант профиля/контекста. Требуется: ${required.joinToString()}, запрошено: $conflict."
    }

    private fun stripControlKeywords(text: String): String {
        return text
            .replace(Regex("(?i)\\bneed context\\b"), "")
            .replace(Regex("(?i)\\bfull context\\b"), "")
            .replace(Regex("(?i)\\bApprove\\b"), "")
            .replace(Regex("(?i)\\bDecline\\b"), "")
            .trim()
    }

    private fun containsNeedContext(text: String): Boolean = Regex("(?i)\\bneed context\\b").containsMatchIn(text)
    private fun containsFullContext(text: String): Boolean = Regex("(?i)\\bfull context\\b").containsMatchIn(text)
    private fun containsApprove(text: String): Boolean = Regex("(?i)\\bapprove\\b").containsMatchIn(text)
    private fun containsDecline(text: String): Boolean = Regex("(?i)\\bdecline\\b").containsMatchIn(text)
}
