package com.aprekek.ai_advent.agentic_app.domain.port

interface ApiKeyRepository {
    suspend fun saveApiKey(profileId: String, apiKey: String)
    suspend fun getApiKey(profileId: String): String?
}
