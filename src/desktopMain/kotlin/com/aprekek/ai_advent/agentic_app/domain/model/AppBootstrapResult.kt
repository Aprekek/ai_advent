package com.aprekek.ai_advent.agentic_app.domain.model

data class AppBootstrapResult(
    val profiles: List<UserProfile>,
    val activeProfileId: String,
    val workspace: ProfileWorkspace,
    val themeMode: ThemeMode,
    val panelLayoutState: PanelLayoutState
)
