package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.AppBootstrapResult
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository

class BootstrapAppUseCase(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository,
    private val loadWorkspaceUseCase: LoadWorkspaceUseCase
) {
    suspend fun execute(): AppBootstrapResult {
        var profiles = userRepository.listProfiles()
        if (profiles.isEmpty()) {
            userRepository.createProfile(name = "Default")
            profiles = userRepository.listProfiles()
        }

        val preferences = preferencesRepository.load()
        val activeProfileId = preferences.activeProfileId
            ?.takeIf { activeId -> profiles.any { it.id == activeId } }
            ?: profiles.first().id

        if (activeProfileId != preferences.activeProfileId) {
            preferencesRepository.setActiveProfile(activeProfileId)
        }

        return AppBootstrapResult(
            profiles = profiles,
            activeProfileId = activeProfileId,
            workspace = loadWorkspaceUseCase.execute(activeProfileId),
            themeMode = preferences.themeMode,
            panelLayoutState = preferences.panelLayoutState
        )
    }
}
