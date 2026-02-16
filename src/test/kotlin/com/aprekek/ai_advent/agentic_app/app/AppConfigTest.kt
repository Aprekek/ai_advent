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
    }

    @Test
    fun `strips wrapping quotes from env values`() {
        val provider = MapEnvironmentProvider(
            mapOf(
                "DEEPSEEK_API_KEY" to "\"dsk_test\"",
                "DEEPSEEK_BASE_URL" to "\"https://api.deepseek.com/v1\"",
                "DEEPSEEK_MODEL" to "'deepseek-chat'",
                "DEEPSEEK_RESPONSE_LANGUAGE" to "\"Russian\""
            )
        )

        val config = AppConfig.fromEnvironment(provider)

        assertEquals("dsk_test", config.apiKey)
        assertEquals("https://api.deepseek.com/v1", config.baseUrl)
        assertEquals("deepseek-chat", config.model)
        assertEquals("Russian", config.responseLanguage)
    }

    private class MapEnvironmentProvider(
        private val source: Map<String, String>
    ) : EnvironmentProvider {
        override fun get(name: String): String? = source[name]
    }
}
