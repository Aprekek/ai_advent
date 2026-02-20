package com.aprekek.ai_advent.agentic_app.app

data class AppConfig(
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val responseLanguage: String,
    val modelV30: String,
    val huggingFaceApiKey: String,
    val huggingFaceBaseUrl: String,
    val huggingFaceModelV30: String,
    val huggingFaceInputCostPer1M: Double? = null,
    val huggingFaceOutputCostPer1M: Double? = null
) {
    companion object {
        private const val ApiKeyEnvName = "DEEPSEEK_API_KEY"
        private const val BaseUrlEnvName = "DEEPSEEK_BASE_URL"
        private const val ModelEnvName = "DEEPSEEK_MODEL"
        private const val ModelV30EnvName = "DEEPSEEK_MODEL_V30"
        private const val ResponseLanguageEnvName = "DEEPSEEK_RESPONSE_LANGUAGE"
        private const val HuggingFaceApiKeyEnvName = "HUGGINGFACE_API_KEY"
        private const val HuggingFaceBaseUrlEnvName = "HUGGINGFACE_BASE_URL"
        private const val HuggingFaceModelV30EnvName = "HUGGINGFACE_MODEL_V30"
        private const val HuggingFaceInputCostPer1MEnvName = "HUGGINGFACE_INPUT_COST_PER_1M"
        private const val HuggingFaceOutputCostPer1MEnvName = "HUGGINGFACE_OUTPUT_COST_PER_1M"

        private const val DefaultBaseUrl = "https://api.deepseek.com/v1"
        private const val DefaultModel = "deepseek-chat"
        private const val DefaultModelV30 = "deepseek-v3"
        private const val DefaultResponseLanguage = "Russian"
        private const val DefaultHuggingFaceBaseUrl = "https://router.huggingface.co/v1"
        private const val DefaultHuggingFaceModelV30 = "deepseek-ai/DeepSeek-V3:novita"

        fun fromEnvironment(environment: EnvironmentProvider = SystemEnvironmentProvider): AppConfig {
            val apiKey = environment.get(ApiKeyEnvName).normalizedEnvValue()
            require(apiKey.isNotEmpty()) {
                "$ApiKeyEnvName is required. Example: export $ApiKeyEnvName=\"dsk_...\""
            }

            val baseUrl = environment.get(BaseUrlEnvName).normalizedEnvValue()
                .ifBlank { DefaultBaseUrl }
            val model = environment.get(ModelEnvName).normalizedEnvValue()
                .ifBlank { DefaultModel }
            val modelV30 = environment.get(ModelV30EnvName).normalizedEnvValue()
                .ifBlank { DefaultModelV30 }
            val responseLanguage = environment.get(ResponseLanguageEnvName).normalizedEnvValue()
                .ifBlank { DefaultResponseLanguage }
            val huggingFaceApiKey = environment.get(HuggingFaceApiKeyEnvName).normalizedEnvValue()
            val huggingFaceBaseUrl = environment.get(HuggingFaceBaseUrlEnvName).normalizedEnvValue()
                .ifBlank { DefaultHuggingFaceBaseUrl }
            val huggingFaceModelV30 = environment.get(HuggingFaceModelV30EnvName).normalizedEnvValue()
                .ifBlank { DefaultHuggingFaceModelV30 }
            val huggingFaceInputCostPer1M = environment.get(HuggingFaceInputCostPer1MEnvName)
                .normalizedEnvValue()
                .toDoubleOrNull()
            val huggingFaceOutputCostPer1M = environment.get(HuggingFaceOutputCostPer1MEnvName)
                .normalizedEnvValue()
                .toDoubleOrNull()

            return AppConfig(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                responseLanguage = responseLanguage,
                modelV30 = modelV30,
                huggingFaceApiKey = huggingFaceApiKey,
                huggingFaceBaseUrl = huggingFaceBaseUrl,
                huggingFaceModelV30 = huggingFaceModelV30,
                huggingFaceInputCostPer1M = huggingFaceInputCostPer1M,
                huggingFaceOutputCostPer1M = huggingFaceOutputCostPer1M
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
