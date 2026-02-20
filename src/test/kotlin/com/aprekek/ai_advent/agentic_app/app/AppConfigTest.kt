package com.aprekek.ai_advent.agentic_app.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigTest {
    @Test
    fun `throws when api key is missing`() {
        val env = mapOf<String, String>()
        val provider = MapEnvironmentProvider(env)

        assertFailsWith<IllegalArgumentException> {
            AppConfig.fromEnvironment(provider)
        }
    }

    @Test
    fun `uses defaults for optional values`() {
        val provider = MapEnvironmentProvider(
            mapOf(
                "DEEPSEEK_API_KEY" to "dsk_test"
            )
        )

        val config = AppConfig.fromEnvironment(provider)

        assertEquals("dsk_test", config.apiKey)
        assertEquals("https://api.deepseek.com/v1", config.baseUrl)
        assertEquals("deepseek-chat", config.model)
        assertEquals("Russian", config.responseLanguage)
        assertEquals("deepseek-v3", config.modelV30)
        assertEquals("", config.huggingFaceApiKey)
        assertEquals("https://router.huggingface.co/v1", config.huggingFaceBaseUrl)
        assertEquals("deepseek-ai/DeepSeek-V3:novita", config.huggingFaceModelV30)
        assertEquals(null, config.huggingFaceInputCostPer1M)
        assertEquals(null, config.huggingFaceOutputCostPer1M)
    }

    @Test
    fun `strips wrapping quotes from env values`() {
        val provider = MapEnvironmentProvider(
            mapOf(
                "DEEPSEEK_API_KEY" to "\"dsk_test\"",
                "DEEPSEEK_BASE_URL" to "\"https://api.deepseek.com/v1\"",
                "DEEPSEEK_MODEL" to "'deepseek-chat'",
                "DEEPSEEK_RESPONSE_LANGUAGE" to "\"Russian\"",
                "DEEPSEEK_MODEL_V30" to "'deepseek-v3'",
                "HUGGINGFACE_API_KEY" to "\"hf_test\"",
                "HUGGINGFACE_BASE_URL" to "\"https://router.huggingface.co/v1\"",
                "HUGGINGFACE_MODEL_V30" to "'deepseek-ai/DeepSeek-V3:novita'",
                "HUGGINGFACE_INPUT_COST_PER_1M" to "\"0.8\"",
                "HUGGINGFACE_OUTPUT_COST_PER_1M" to "'1.2'"
            )
        )

        val config = AppConfig.fromEnvironment(provider)

        assertEquals("dsk_test", config.apiKey)
        assertEquals("https://api.deepseek.com/v1", config.baseUrl)
        assertEquals("deepseek-chat", config.model)
        assertEquals("Russian", config.responseLanguage)
        assertEquals("deepseek-v3", config.modelV30)
        assertEquals("hf_test", config.huggingFaceApiKey)
        assertEquals("https://router.huggingface.co/v1", config.huggingFaceBaseUrl)
        assertEquals("deepseek-ai/DeepSeek-V3:novita", config.huggingFaceModelV30)
        assertEquals(0.8, config.huggingFaceInputCostPer1M)
        assertEquals(1.2, config.huggingFaceOutputCostPer1M)
    }

    private class MapEnvironmentProvider(
        private val source: Map<String, String>
    ) : EnvironmentProvider {
        override fun get(name: String): String? = source[name]
    }
}
