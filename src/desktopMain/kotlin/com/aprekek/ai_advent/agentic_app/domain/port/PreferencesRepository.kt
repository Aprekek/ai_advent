package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.AppPreferences
import com.aprekek.ai_advent.agentic_app.domain.model.PanelLayoutState
import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode

interface PreferencesRepository {
    suspend fun load(): AppPreferences
    suspend fun setActiveProfile(profileId: String)
    suspend fun setActiveChat(profileId: String, chatId: String)
    suspend fun setTheme(themeMode: ThemeMode)
    suspend fun setPanelLayout(layoutState: PanelLayoutState)
}
