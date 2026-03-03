package com.aprekek.ai_advent.agentic_app.data.remote.deepseek

import com.aprekek.ai_advent.agentic_app.util.defaultJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeepSeekSseParserTest {
    private val parser = DeepSeekSseParser(defaultJson())

    @Test
    fun `parses delta content`() {
        val parsed = parser.parseDataPayload(
            """{"choices":[{"delta":{"content":"Hello"}}]}"""
        )

        assertEquals(ParsedSsePayload.Delta("Hello"), parsed)
    }

    @Test
    fun `parses done token`() {
        val parsed = parser.parseDataPayload("[DONE]")
        assertEquals(ParsedSsePayload.Done, parsed)
    }

    @Test
    fun `throws on error payload`() {
        assertFailsWith<IllegalStateException> {
            parser.parseDataPayload("""{"error":{"message":"bad request"}}""")
        }
    }
}
