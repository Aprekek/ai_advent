package com.aprekek.ai_advent.agentic_app.app

data class AppConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val responseLanguage: String
) {
    companion object {
        private const val ApiKeyEnvName = "DEEPSEEK_API_KEY"
        private const val BaseUrlEnvName = "DEEPSEEK_BASE_URL"
        private const val ModelEnvName = "DEEPSEEK_MODEL"
        private const val ResponseLanguageEnvName = "DEEPSEEK_RESPONSE_LANGUAGE"

        private const val DefaultBaseUrl = "https://api.deepseek.com/v1"
        private const val DefaultModel = "deepseek-chat"
        private const val DefaultResponseLanguage = "Russian"

        fun fromEnvironment(environment: EnvironmentProvider = SystemEnvironmentProvider): AppConfig {
            val apiKey = environment.get(ApiKeyEnvName).normalizedEnvValue()
            require(apiKey.isNotEmpty()) {
                "$ApiKeyEnvName is required. Example: export $ApiKeyEnvName=\"dsk_...\""
            }

            val baseUrl = environment.get(BaseUrlEnvName).normalizedEnvValue()
                .ifBlank { DefaultBaseUrl }
            val model = environment.get(ModelEnvName).normalizedEnvValue()
                .ifBlank { DefaultModel }
            val responseLanguage = environment.get(ResponseLanguageEnvName).normalizedEnvValue()
                .ifBlank { DefaultResponseLanguage }

            return AppConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                responseLanguage = responseLanguage
            )
        }

        private fun String?.normalizedEnvValue(): String {
            val trimmed = this?.trim().orEmpty()
            if (trimmed.length >= 2) {
                val startsWithDoubleQuote = trimmed.first() == '"' && trimmed.last() == '"'
                val startsWithSingleQuote = trimmed.first() == '\'' && trimmed.last() == '\''
                if (startsWithDoubleQuote || startsWithSingleQuote) {
                    return trimmed.substring(1, trimmed.length - 1).trim()
                }
            }
            return trimmed
        }
    }
}

interface EnvironmentProvider {
    fun get(name: String): String?
}

object SystemEnvironmentProvider : EnvironmentProvider {
    override fun get(name: String): String? = System.getenv(name)
}
