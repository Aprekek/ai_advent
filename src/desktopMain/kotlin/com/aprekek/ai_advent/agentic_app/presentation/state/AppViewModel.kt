package com.aprekek.ai_advent.agentic_app.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aprekek.ai_advent.agentic_app.domain.model.ApiError
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.model.SendMessageProgress
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineAction
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineProgress
import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode
import com.aprekek.ai_advent.agentic_app.domain.usecase.BootstrapAppUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CreateChatUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CreateProfileUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.DeleteChatUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.DeleteProfileUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.LoadWorkspaceUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SaveApiKeyUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SelectChatUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SetPanelLayoutUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SetThemeUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.StateMachineChatUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SwitchProfileUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.UpdateChatContextUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.UpdateProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppViewModel(
    private val bootstrapAppUseCase: BootstrapAppUseCase,
    private val createProfileUseCase: CreateProfileUseCase,
    private val deleteProfileUseCase: DeleteProfileUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val switchProfileUseCase: SwitchProfileUseCase,
    private val loadWorkspaceUseCase: LoadWorkspaceUseCase,
    private val createChatUseCase: CreateChatUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val selectChatUseCase: SelectChatUseCase,
    private val updateChatContextUseCase: UpdateChatContextUseCase,
    private val saveApiKeyUseCase: SaveApiKeyUseCase,
    private val setThemeUseCase: SetThemeUseCase,
    private val setPanelLayoutUseCase: SetPanelLayoutUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val stateMachineChatUseCase: StateMachineChatUseCase
) {
    var state by mutableStateOf(AppUiState())
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var streamJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            runCatching {
                val bootstrap = bootstrapAppUseCase.execute()
                state = state.copy(
                    isLoading = false,
                    profiles = bootstrap.profiles,
                    activeProfileId = bootstrap.activeProfileId,
                    chats = bootstrap.workspace.chats,
                    selectedChatId = bootstrap.workspace.selectedChatId,
                    selectedChatMode = bootstrap.workspace.selectedChatMode,
                    stateMachineSession = bootstrap.workspace.stateMachineSession,
                    messages = bootstrap.workspace.messages,
                    hasApiKey = bootstrap.workspace.hasApiKey,
                    themeMode = bootstrap.themeMode,
                    panelLayoutState = bootstrap.panelLayoutState,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(isLoading = false, errorMessage = userMessageForError(error))
            }
        }
    }

    fun createProfile(name: String, descriptionItems: List<String>) {
        scope.launch {
            runCatching {
                val profile = createProfileUseCase.execute(name, descriptionItems)
                val workspace = switchProfileUseCase.execute(profile.id)
                state = state.copy(
                    profiles = state.profiles + profile,
                    activeProfileId = profile.id,
                    chats = workspace.chats,
                    selectedChatId = workspace.selectedChatId,
                    selectedChatMode = workspace.selectedChatMode,
                    stateMachineSession = workspace.stateMachineSession,
                    messages = workspace.messages,
                    hasApiKey = workspace.hasApiKey,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun updateProfile(profileId: String, name: String, descriptionItems: List<String>) {
        scope.launch {
            runCatching {
                updateProfileUseCase.execute(profileId, name, descriptionItems)
                val bootstrap = bootstrapAppUseCase.execute()
                state = state.copy(profiles = bootstrap.profiles, activeProfileId = bootstrap.activeProfileId, errorMessage = null)
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun deleteProfile(profileId: String) {
        scope.launch {
            runCatching {
                if (state.isStreaming && state.activeProfileId == profileId) {
                    stopStreaming()
                }
                val newActiveProfile = deleteProfileUseCase.execute(profileId)
                val profiles = bootstrapAppUseCase.execute().profiles
                val workspace = loadWorkspaceUseCase.execute(newActiveProfile.id)

                state = state.copy(
                    profiles = profiles,
                    activeProfileId = newActiveProfile.id,
                    chats = workspace.chats,
                    selectedChatId = workspace.selectedChatId,
                    selectedChatMode = workspace.selectedChatMode,
                    stateMachineSession = workspace.stateMachineSession,
                    messages = workspace.messages,
                    hasApiKey = workspace.hasApiKey,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun switchProfile(profileId: String) {
        scope.launch {
            runCatching {
                val workspace = switchProfileUseCase.execute(profileId)
                state = state.copy(
                    activeProfileId = profileId,
                    chats = workspace.chats,
                    selectedChatId = workspace.selectedChatId,
                    selectedChatMode = workspace.selectedChatMode,
                    stateMachineSession = workspace.stateMachineSession,
                    messages = workspace.messages,
                    hasApiKey = workspace.hasApiKey,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun createChat(mode: ChatMode) {
        val profileId = state.activeProfileId ?: return
        scope.launch {
            runCatching {
                createChatUseCase.execute(profileId = profileId, mode = mode)
                refreshWorkspace(profileId)
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun selectChat(chatId: String) {
        val profileId = state.activeProfileId ?: return
        scope.launch {
            runCatching {
                selectChatUseCase.execute(profileId, chatId)
                refreshWorkspace(profileId)
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun deleteChat(chatId: String) {
        val profileId = state.activeProfileId ?: return
        scope.launch {
            runCatching {
                if (state.isStreaming && state.selectedChatId == chatId) {
                    stopStreaming()
                }
                deleteChatUseCase.execute(profileId, chatId)
                refreshWorkspace(profileId)
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun updateSelectedChatContextItems(contextItems: List<String>) {
        val profileId = state.activeProfileId ?: return
        val chatId = state.selectedChatId ?: return
        scope.launch {
            runCatching {
                updateChatContextUseCase.execute(profileId, chatId, contextItems)
                refreshWorkspace(profileId)
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun saveApiKey(value: String) {
        val profileId = state.activeProfileId ?: return
        scope.launch {
            runCatching {
                saveApiKeyUseCase.execute(profileId, value)
                refreshWorkspace(profileId)
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun toggleTheme() {
        val newTheme = if (state.themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
        state = state.copy(themeMode = newTheme)
        scope.launch {
            runCatching { setThemeUseCase.execute(newTheme) }
                .onFailure { error -> state = state.copy(errorMessage = userMessageForError(error)) }
        }
    }

    fun updatePanelLayout(layout: PanelLayoutState) {
        state = state.copy(panelLayoutState = layout)
        scope.launch {
            runCatching { setPanelLayoutUseCase.execute(layout) }
                .onFailure { error -> state = state.copy(errorMessage = userMessageForError(error)) }
        }
    }

    fun sendMessage(input: String) {
        if (state.isStreaming) return

        val profileId = state.activeProfileId ?: return
        val normalizedInput = input.trim()
        if (normalizedInput.isEmpty()) return

        scope.launch {
            val selectedChatId = runCatching {
                state.selectedChatId ?: createChatUseCase.execute(profileId, mode = state.selectedChatMode).id
            }.getOrElse { error ->
                state = state.copy(errorMessage = userMessageForError(error))
                return@launch
            }

            if (state.selectedChatMode == ChatMode.STATE_MACHINE) {
                runStateMachineAction(profileId, selectedChatId, StateMachineAction.UserInput(normalizedInput))
            } else {
                runStandardStream(profileId, selectedChatId, normalizedInput)
            }
        }
    }

    fun stateMachineApprovePlan() = runStateMachineControl(StateMachineAction.ApprovePlan)
    fun stateMachineSkipClarification() = runStateMachineControl(StateMachineAction.SkipClarificationToExecution)
    fun stateMachineContinue() = runStateMachineControl(StateMachineAction.Continue)
    fun stateMachineValidationRework() = runStateMachineControl(StateMachineAction.ValidationRework)
    fun stateMachineValidationAcceptCurrent() = runStateMachineControl(StateMachineAction.ValidationAcceptCurrent)

    fun stopStreaming() {
        val profileId = state.activeProfileId
        val chatId = state.selectedChatId
        val isFsm = state.selectedChatMode == ChatMode.STATE_MACHINE

        streamJob?.cancel()
        streamJob = null
        state = state.copy(isStreaming = false, isAwaitingFirstToken = false, reconnectAttempt = null)

        if (isFsm && profileId != null && chatId != null) {
            scope.launch {
                runCatching {
                    stateMachineChatUseCase.execute(profileId, chatId, StateMachineAction.Stop).collect { }
                    refreshWorkspace(profileId)
                }.onFailure { error ->
                    state = state.copy(errorMessage = userMessageForError(error))
                }
            }
        }
    }

    fun clearError() {
        state = state.copy(errorMessage = null)
    }

    fun dispose() {
        scope.cancel()
    }

    private fun runStateMachineControl(action: StateMachineAction) {
        val profileId = state.activeProfileId ?: return
        val chatId = state.selectedChatId ?: return
        if (state.selectedChatMode != ChatMode.STATE_MACHINE || state.isStreaming) return

        scope.launch {
            runStateMachineAction(profileId, chatId, action)
        }
    }

    private suspend fun runStateMachineAction(
        profileId: String,
        chatId: String,
        action: StateMachineAction
    ) {
        val shouldShowStreaming = action is StateMachineAction.UserInput || action == StateMachineAction.Continue
        val baseMessages = state.messages
        state = state.copy(
            isStreaming = shouldShowStreaming,
            isAwaitingFirstToken = shouldShowStreaming,
            reconnectAttempt = null,
            errorMessage = null
        )

        streamJob = scope.launch {
            runCatching {
                stateMachineChatUseCase.execute(profileId, chatId, action).collect { progress ->
                    when (progress) {
                        is StateMachineProgress.PartialAssistant -> {
                            val assistantMessage = ChatMessage(
                                id = "local-assistant-fsm",
                                chatId = chatId,
                                role = ChatRole.ASSISTANT,
                                content = progress.content,
                                createdAt = System.currentTimeMillis()
                            )
                            state = state.copy(
                                messages = baseMessages + assistantMessage,
                                isAwaitingFirstToken = false,
                                reconnectAttempt = null
                            )
                        }
                        is StateMachineProgress.Reconnecting -> {
                            state = state.copy(reconnectAttempt = progress.attempt)
                        }
                        is StateMachineProgress.SessionUpdated -> {
                            state = state.copy(stateMachineSession = progress.session)
                        }
                        StateMachineProgress.Completed -> Unit
                    }
                }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    state = state.copy(errorMessage = userMessageForError(error))
                }
            }

            refreshWorkspace(profileId)
            state = state.copy(isStreaming = false, isAwaitingFirstToken = false, reconnectAttempt = null)
        }
    }

    private suspend fun runStandardStream(profileId: String, chatId: String, normalizedInput: String) {
        val localUserMessage = ChatMessage(
            id = "local-user",
            chatId = chatId,
            role = ChatRole.USER,
            content = normalizedInput,
            createdAt = System.currentTimeMillis()
        )

        state = state.copy(
            isStreaming = true,
            isAwaitingFirstToken = true,
            reconnectAttempt = null,
            selectedChatId = chatId,
            messages = state.messages + localUserMessage,
            errorMessage = null
        )
        val liveMessages = state.messages

        streamJob = scope.launch {
            runCatching {
                sendMessageUseCase.execute(profileId = profileId, chatId = chatId, userInput = normalizedInput)
                    .collect { progress ->
                        when (progress) {
                            is SendMessageProgress.PartialAssistant -> {
                                val assistantMessage = ChatMessage(
                                    id = "local-assistant",
                                    chatId = chatId,
                                    role = ChatRole.ASSISTANT,
                                    content = progress.content,
                                    createdAt = System.currentTimeMillis()
                                )
                                state = state.copy(
                                    messages = liveMessages + assistantMessage,
                                    isAwaitingFirstToken = false,
                                    reconnectAttempt = null
                                )
                            }
                            is SendMessageProgress.Reconnecting -> {
                                state = state.copy(reconnectAttempt = progress.attempt)
                            }
                            SendMessageProgress.Completed -> Unit
                        }
                    }
            }.onFailure { error ->
                if (error !is CancellationException) {
                    state = state.copy(errorMessage = userMessageForError(error))
                }
            }

            refreshWorkspace(profileId)
            state = state.copy(isStreaming = false, isAwaitingFirstToken = false, reconnectAttempt = null)
        }
    }

    private suspend fun refreshWorkspace(profileId: String) {
        val workspace = loadWorkspaceUseCase.execute(profileId)
        state = state.copy(
            chats = workspace.chats,
            selectedChatId = workspace.selectedChatId,
            selectedChatMode = workspace.selectedChatMode,
            stateMachineSession = workspace.stateMachineSession,
            messages = workspace.messages,
            hasApiKey = workspace.hasApiKey
        )
    }

    private fun userMessageForError(error: Throwable): String {
        return when (error) {
            is ApiError -> error.message ?: "Ошибка API"
            else -> error.message ?: "Неизвестная ошибка"
        }
    }
}
