package com.aprekek.ai_advent.agentic_app.data.provider.deepseek

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class DeepSeekApiClientTest {
    @Test
    fun `returns message content on success`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer dsk_test_key", request.headers[HttpHeaders.Authorization])
            assertEquals("https://api.deepseek.com/v1/chat/completions", request.url.toString())

            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"Hello"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = DeepSeekApiClient(httpClient = testHttpClient(engine))

        val result = client.sendChatCompletion(
            request = DeepSeekChatCompletionRequest(
                model = "deepseek-chat",
                messages = listOf(DeepSeekMessage(role = "user", content = "Hi"))
            ),
            requestContext = ProviderRequestContext(
                model = "deepseek-chat",
                apiKey = "dsk_test_key",
                baseUrl = "https://api.deepseek.com/v1"
            )
        )

        assertEquals("Hello", result.choices.single().message.content)
    }

    @Test
    fun `throws readable error on unauthorized`() = runTest {
        val engine = MockEngine {
            respond(
                content = """{"error":{"message":"Invalid API key"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val client = DeepSeekApiClient(httpClient = testHttpClient(engine))

        val exception = assertFailsWith<IllegalStateException> {
            client.sendChatCompletion(
                request = DeepSeekChatCompletionRequest(
                    model = "deepseek-chat",
                    messages = listOf(DeepSeekMessage(role = "user", content = "Hi"))
                ),
                requestContext = ProviderRequestContext(
                    model = "deepseek-chat",
                    apiKey = "bad_key",
                    baseUrl = "https://api.deepseek.com/v1"
                )
            )
        }

        assertContains(exception.message.orEmpty(), "HTTP 401")
        assertContains(exception.message.orEmpty(), "Invalid API key")
    }

    private fun testHttpClient(engine: MockEngine): HttpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }
}
