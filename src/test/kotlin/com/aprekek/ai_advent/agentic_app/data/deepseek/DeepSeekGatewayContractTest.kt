package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
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
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class DeepSeekGatewayContractTest {
    @Test
    fun `uses default provider context from app config`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer dsk_default", request.headers[HttpHeaders.Authorization])
            assertEquals("https://api.deepseek.com/v1/chat/completions", request.url.toString())
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"default"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val gateway = DeepSeekGateway(
            apiClient = DeepSeekApiClient(testHttpClient(engine)),
            config = testConfig()
        )

        val result = gateway.generate(
            messages = listOf(ChatMessage(role = ChatRole.User, content = "Hi")),
            options = GenerationOptions.Standard
        )

        assertEquals("default", result)
    }

    @Test
    fun `uses explicit provider context when passed`() = runTest {
        val engine = MockEngine { request ->
            assertEquals("Bearer hf_key", request.headers[HttpHeaders.Authorization])
            assertEquals("https://router.huggingface.co/v1/chat/completions", request.url.toString())
            respond(
                content = """{"choices":[{"message":{"role":"assistant","content":"override"}}]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val gateway = DeepSeekGateway(
            apiClient = DeepSeekApiClient(testHttpClient(engine)),
            config = testConfig()
        )

        val result = gateway.generateWithContext(
            messages = listOf(ChatMessage(role = ChatRole.User, content = "Hi")),
            options = GenerationOptions.Standard,
            requestContext = ProviderRequestContext(
                model = "deepseek-ai/DeepSeek-V3:novita",
                apiKey = "hf_key",
                baseUrl = "https://router.huggingface.co/v1"
            )
        )

        assertEquals("override", result)
    }

    private fun testConfig(): AppConfig = AppConfig(
        apiKey = "dsk_default",
        baseUrl = "https://api.deepseek.com/v1",
        model = "deepseek-chat",
        responseLanguage = "Russian",
        modelV30 = "deepseek-v3",
        huggingFaceApiKey = "",
        huggingFaceBaseUrl = "https://router.huggingface.co/v1",
        huggingFaceModelV30 = "deepseek-ai/DeepSeek-V3:novita"
    )

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
