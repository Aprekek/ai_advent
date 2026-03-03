package com.aprekek.ai_advent.agentic_app.data.local

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatThread
import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode
import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile
import kotlinx.serialization.Serializable

@Serializable
data class AppStateFile(
    val profiles: List<UserProfile> = emptyList(),
    val activeProfileId: String? = null,
    val activeChatByProfile: Map<String, String> = emptyMap(),
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val panelLayoutState: PanelLayoutState = PanelLayoutState()
)

@Serializable
data class ProfileDataFile(
    val chats: List<ChatThread> = emptyList(),
    val messagesByChat: Map<String, List<ChatMessage>> = emptyMap()
)
