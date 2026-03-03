package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ThemeMode
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class SetThemeUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(themeMode: ThemeMode) {
        preferencesRepository.setTheme(themeMode)
    }
}
