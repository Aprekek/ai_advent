package com.aprekek.ai_advent.agentic_app.presentation.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aprekek.ai_advent.agentic_app.domain.model.ApiError
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.model.SendMessageProgress
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
import com.aprekek.ai_advent.agentic_app.domain.usecase.SwitchProfileUseCase
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
    private val switchProfileUseCase: SwitchProfileUseCase,
    private val loadWorkspaceUseCase: LoadWorkspaceUseCase,
    private val createChatUseCase: CreateChatUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val selectChatUseCase: SelectChatUseCase,
    private val saveApiKeyUseCase: SaveApiKeyUseCase,
    private val setThemeUseCase: SetThemeUseCase,
    private val setPanelLayoutUseCase: SetPanelLayoutUseCase,
    private val sendMessageUseCase: SendMessageUseCase
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
                    messages = bootstrap.workspace.messages,
                    hasApiKey = bootstrap.workspace.hasApiKey,
                    themeMode = bootstrap.themeMode,
                    panelLayoutState = bootstrap.panelLayoutState,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(
                    isLoading = false,
                    errorMessage = userMessageForError(error)
                )
            }
        }
    }

    fun createProfile(name: String) {
        scope.launch {
            runCatching {
                val profile = createProfileUseCase.execute(name)
                val workspace = switchProfileUseCase.execute(profile.id)
                state = state.copy(
                    profiles = state.profiles + profile,
                    activeProfileId = profile.id,
                    chats = workspace.chats,
                    selectedChatId = workspace.selectedChatId,
                    messages = workspace.messages,
                    hasApiKey = workspace.hasApiKey,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun deleteActiveProfile() {
        val profileId = state.activeProfileId ?: return
        scope.launch {
            runCatching {
                if (state.isStreaming) {
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
                    messages = workspace.messages,
                    hasApiKey = workspace.hasApiKey,
                    errorMessage = null
                )
            }.onFailure { error ->
                state = state.copy(errorMessage = userMessageForError(error))
            }
        }
    }

    fun createChat() {
        val profileId = state.activeProfileId ?: return
        scope.launch {
            runCatching {
                createChatUseCase.execute(profileId)
                val workspace = loadWorkspaceUseCase.execute(profileId)
                state = state.copy(
                    chats = workspace.chats,
                    selectedChatId = workspace.selectedChatId,
                    messages = workspace.messages,
                    errorMessage = null
                )
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
                val workspace = loadWorkspaceUseCase.execute(profileId)
                state = state.copy(
                    selectedChatId = workspace.selectedChatId,
                    messages = workspace.messages,
                    errorMessage = null
                )
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
                val workspace = loadWorkspaceUseCase.execute(profileId)
                state = state.copy(
                    chats = workspace.chats,
                    selectedChatId = workspace.selectedChatId,
                    messages = workspace.messages,
                    errorMessage = null
                )
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
                val workspace = loadWorkspaceUseCase.execute(profileId)
                state = state.copy(hasApiKey = workspace.hasApiKey, errorMessage = null)
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
        scope.launch {
            val selectedChatId = runCatching {
                state.selectedChatId ?: createChatUseCase.execute(profileId).id
            }.getOrElse { error ->
                state = state.copy(errorMessage = userMessageForError(error))
                return@launch
            }

            val normalizedInput = input.trim()
            if (normalizedInput.isEmpty()) return@launch

            val localUserMessage = ChatMessage(
                id = "local-user",
                chatId = selectedChatId,
                role = ChatRole.USER,
                content = normalizedInput,
                createdAt = System.currentTimeMillis()
            )

            state = state.copy(
                isStreaming = true,
                isAwaitingFirstToken = true,
                reconnectAttempt = null,
                selectedChatId = selectedChatId,
                messages = state.messages + localUserMessage,
                errorMessage = null
            )
            val liveMessages = state.messages

            streamJob = scope.launch {
                runCatching {
                    sendMessageUseCase.execute(
                        profileId = profileId,
                        chatId = selectedChatId,
                        userInput = normalizedInput
                    ).collect { progress ->
                        when (progress) {
                            is SendMessageProgress.PartialAssistant -> {
                                val assistantMessage = ChatMessage(
                                    id = "local-assistant",
                                    chatId = selectedChatId,
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

                val workspace = runCatching { loadWorkspaceUseCase.execute(profileId) }.getOrNull()
                if (workspace != null) {
                    state = state.copy(
                        chats = workspace.chats,
                        selectedChatId = workspace.selectedChatId,
                        messages = workspace.messages,
                        hasApiKey = workspace.hasApiKey,
                        reconnectAttempt = null,
                        isAwaitingFirstToken = false,
                        isStreaming = false
                    )
                } else {
                    state = state.copy(
                        isStreaming = false,
                        isAwaitingFirstToken = false,
                        reconnectAttempt = null
                    )
                }
            }
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        streamJob = null
        state = state.copy(isStreaming = false, isAwaitingFirstToken = false, reconnectAttempt = null)
    }

    fun clearError() {
        state = state.copy(errorMessage = null)
    }

    fun dispose() {
        scope.cancel()
    }

    private fun userMessageForError(error: Throwable): String {
        return when (error) {
            is ApiError -> error.message ?: "Ошибка API"
            else -> error.message ?: "Неизвестная ошибка"
        }
    }
}
