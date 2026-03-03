package com.aprekek.ai_advent.agentic_app.data.local

import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile
import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider
import com.aprekek.ai_advent.agentic_app.domain.port.UserRepository

class FileUserRepository(
    private val appStateStore: AppStateStore,
    private val idGenerator: IdGenerator,
    private val timeProvider: TimeProvider
) : UserRepository {
    override suspend fun listProfiles(): List<UserProfile> {
        return appStateStore.read().profiles.sortedBy { it.createdAt }
    }

    override suspend fun createProfile(name: String): UserProfile {
        val profile = UserProfile(
            id = idGenerator.nextId(),
            name = name,
            createdAt = timeProvider.nowMillis()
        )
        appStateStore.update { current ->
            current.copy(profiles = current.profiles + profile)
        }
        return profile
    }
}
