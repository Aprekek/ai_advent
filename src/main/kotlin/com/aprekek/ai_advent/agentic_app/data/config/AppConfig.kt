package com.aprekek.ai_advent.agentic_app.data.config

data class AppConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val responseLanguage: String,
    val modelV30: String,
    val huggingFaceApiKey: String,
    val huggingFaceBaseUrl: String,
    val huggingFaceModelV30: String
)

interface EnvironmentProvider {
    fun get(name: String): String?
}

object SystemEnvironmentProvider : EnvironmentProvider {
    override fun get(name: String): String? = System.getenv(name)
}
