package com.aprekek.ai_advent.agentic_app.data.local

import com.aprekek.ai_advent.agentic_app.domain.model.AppPreferences
import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class FilePreferencesRepository(
    private val appStateStore: AppStateStore
) : PreferencesRepository {
    override suspend fun load(): AppPreferences {
        val state = appStateStore.read()
        return AppPreferences(
            activeProfileId = state.activeProfileId,
            activeChatByProfile = state.activeChatByProfile,
            themeMode = state.themeMode,
            panelLayoutState = state.panelLayoutState
        )
    }

    override suspend fun setActiveProfile(profileId: String) {
        appStateStore.update { state ->
            state.copy(activeProfileId = profileId)
        }
    }

    override suspend fun setActiveChat(profileId: String, chatId: String) {
        appStateStore.update { state ->
            state.copy(
                activeChatByProfile = state.activeChatByProfile + (profileId to chatId)
            )
        }
    }

    override suspend fun setTheme(themeMode: ThemeMode) {
        appStateStore.update { state ->
            state.copy(themeMode = themeMode)
        }
    }

    override suspend fun setPanelLayout(layoutState: PanelLayoutState) {
        appStateStore.update { state ->
            state.copy(panelLayoutState = layoutState)
        }
    }
}
