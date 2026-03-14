package com.aprekek.ai_advent.agentic_app.presentation.state

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMode
import com.aprekek.ai_advent.agentic_app.domain.model.StateMachineSession
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode
import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile

data class AppUiState(
    val isLoading: Boolean = true,
    val profiles: List<UserProfile> = emptyList(),
    val activeProfileId: String? = null,
    val chats: List<ChatThread> = emptyList(),
    val selectedChatId: String? = null,
    val selectedChatMode: ChatMode = ChatMode.STANDARD,
    val stateMachineSession: StateMachineSession? = null,
    val messages: List<ChatMessage> = emptyList(),
    val hasApiKey: Boolean = false,
    val isStreaming: Boolean = false,
    val isAwaitingFirstToken: Boolean = false,
    val reconnectAttempt: Int? = null,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val panelLayoutState: PanelLayoutState = PanelLayoutState(),
    val errorMessage: String? = null
)
