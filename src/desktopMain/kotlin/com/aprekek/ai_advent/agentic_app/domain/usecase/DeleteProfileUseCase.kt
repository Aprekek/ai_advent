package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository

class DeleteProfileUseCase(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(profileId: String): UserProfile {
        val preferences = preferencesRepository.load()
        userRepository.deleteProfile(profileId)
        preferencesRepository.clearProfileState(profileId)

        var profiles = userRepository.listProfiles()
        if (profiles.isEmpty()) {
            userRepository.createProfile("Default")
            profiles = userRepository.listProfiles()
        }

        val newActive = preferences.activeProfileId
            ?.takeIf { it != profileId }
            ?.let { activeId -> profiles.firstOrNull { it.id == activeId } }
            ?: profiles.first()
        preferencesRepository.setActiveProfile(newActive.id)
        return newActive
    }
}
