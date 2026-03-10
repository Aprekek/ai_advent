package com.aprekek.ai_advent.agentic_app.data.local

import com.aprekek.ai_advent.agentic_app.domain.model.ProfileDescriptionItem
import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository

class FileUserRepository(
    private val appStateStore: AppStateStore,
    private val appDirectories: AppDirectories,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : UserRepository {
    override suspend fun listProfiles(): List<UserProfile> {
        return appStateStore.read().profiles.sortedBy { it.createdAt }
    }

    override suspend fun createProfile(name: String, descriptionItems: List<String>): UserProfile {
        val profile = UserProfile(
            id = idGenerator.nextId(),
            name = name,
            createdAt = timeProvider.nowMillis(),
            descriptionItems = descriptionItems.map(::toDescriptionItem)
        )
        appStateStore.update { current ->
            current.copy(profiles = current.profiles + profile)
        }
        return profile
    }

    override suspend fun updateProfile(profile: UserProfile): UserProfile {
        appStateStore.update { current ->
            current.copy(
                profiles = current.profiles.map { existing ->
                    if (existing.id == profile.id) profile else existing
                }
            )
        }
        return profile
    }

    override suspend fun deleteProfile(profileId: String) {
        appStateStore.update { current ->
            current.copy(
                profiles = current.profiles.filterNot { it.id == profileId }
            )
        }
        appDirectories.deleteProfileDirectory(profileId)
    }

    private fun toDescriptionItem(value: String): ProfileDescriptionItem {
        return ProfileDescriptionItem(
            id = idGenerator.nextId(),
            value = value,
            createdAt = timeProvider.nowMillis()
        )
    }
}
