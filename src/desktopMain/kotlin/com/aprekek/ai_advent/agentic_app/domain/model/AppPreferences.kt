package com.aprekek.ai_advent.agentic_app.domain.model

data class AppPreferences(
    val activeProfileId: String?,
    val activeChatByProfile: Map<String, String>,
    val themeMode: ThemeMode,
    val panelLayoutState: PanelLayoutState
)
