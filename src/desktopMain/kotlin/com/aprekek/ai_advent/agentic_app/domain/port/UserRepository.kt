package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.UserProfile

interface UserRepository {
    suspend fun listProfiles(): List<UserProfile>
    suspend fun createProfile(name: String, descriptionItems: List<String> = emptyList()): UserProfile
    suspend fun updateProfile(profile: UserProfile): UserProfile
    suspend fun deleteProfile(profileId: String)
}
