package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile
import com.aprekek.ai_advent.agentic_app.domain.port.PreferencesRepository
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository

class CreateProfileUseCase(
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend fun execute(name: String, descriptionItems: List<String>): UserProfile {
        val trimmedName = name.trim()
        require(trimmedName.isNotEmpty()) { "Имя пользователя не должно быть пустым" }
        val normalizedDescriptions = descriptionItems
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val profile = userRepository.createProfile(trimmedName, normalizedDescriptions)
        preferencesRepository.setActiveProfile(profile.id)
        return profile
    }
}
