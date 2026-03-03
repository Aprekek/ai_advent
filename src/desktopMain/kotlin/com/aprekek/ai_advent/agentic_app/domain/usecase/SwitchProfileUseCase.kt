package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ProfileWorkspace
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository

class SwitchProfileUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val loadWorkspaceUseCase: LoadWorkspaceUseCase
) {
    suspend fun execute(profileId: String): ProfileWorkspace {
        preferencesRepository.setActiveProfile(profileId)
        return loadWorkspaceUseCase.execute(profileId)
    }
}
