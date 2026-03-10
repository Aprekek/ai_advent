package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.model.ProfileDescriptionItem
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository

class UpdateProfileUseCase(
    private val userRepository: UserRepository,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) {
    suspend fun execute(profileId: String, name: String, descriptionItems: List<String>) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty()) { "Имя пользователя не должно быть пустым" }

        val profiles = userRepository.listProfiles()
        val current = profiles.firstOrNull { it.id == profileId }
            ?: error("Профиль не найден")

        val normalizedDescriptions = descriptionItems
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val existingByValue = current.descriptionItems.associateBy { it.value }
        val updatedDescriptionItems = normalizedDescriptions.map { value ->
            existingByValue[value] ?: ProfileDescriptionItem(
                id = idGenerator.nextId(),
                value = value,
                createdAt = timeProvider.nowMillis()
            )
        }

        userRepository.updateProfile(
            current.copy(
                name = normalizedName,
                descriptionItems = updatedDescriptionItems
            )
        )
    }
}
