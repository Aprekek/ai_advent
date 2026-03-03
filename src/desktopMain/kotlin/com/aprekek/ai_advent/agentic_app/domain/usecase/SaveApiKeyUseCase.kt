package com.aprekek.ai_advent.agentic_app.domain.usecase

import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository

class SaveApiKeyUseCase(
    private val apiKeyRepository: ApiKeyRepository
) {
    suspend fun execute(profileId: String, apiKey: String) {
        val normalized = apiKey.trim()
        require(normalized.isNotEmpty()) { "API key не должен быть пустым" }
        apiKeyRepository.saveApiKey(profileId, normalized)
    }
}
