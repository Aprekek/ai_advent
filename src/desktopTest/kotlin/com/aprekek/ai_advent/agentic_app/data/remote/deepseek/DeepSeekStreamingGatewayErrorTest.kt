package com.aprekek.ai_advent.agentic_app.data.remote.deepseek

import com.aprekek.ai_advent.agentic_app.domain.model.ApiError
import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRole
import com.aprekek.ai_advent.agentic_app.util.defaultJson
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
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

class DeepSeekStreamingGatewayErrorTest {
    @Test
    fun `maps 401 to unauthorized`() = runBlocking {
        val gateway = gatewayWithStatus(HttpStatusCode.Unauthorized)
        assertFailsWith<ApiError.Unauthorized> {
            gateway.streamChat(request()).collect()
        }
    }

    @Test
    fun `maps 429 to rate limit`() = runBlocking {
        val gateway = gatewayWithStatus(HttpStatusCode.TooManyRequests)
        assertFailsWith<ApiError.RateLimited> {
            gateway.streamChat(request()).collect()
        }
    }

    @Test
    fun `maps 5xx to server error`() = runBlocking {
        val gateway = gatewayWithStatus(HttpStatusCode.BadGateway)
        assertFailsWith<ApiError.Server> {
            gateway.streamChat(request()).collect()
        }
    }

    private fun gatewayWithStatus(status: HttpStatusCode): DeepSeekStreamingGateway {
        val engine = MockEngine {
            respond(
                content = """{"error":{"message":"boom"}}""",
                status = status,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }

        val json = defaultJson()
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(json)
            }
        }

        return DeepSeekStreamingGateway(
            httpClient = client,
            json = json,
            maxRetries = 0
        )
    }

    private fun request(): ChatRequest {
        return ChatRequest(
            apiKey = "k",
            messages = listOf(
                ChatMessage(
                    id = "m1",
                    chatId = "c1",
                    role = ChatRole.USER,
                    content = "hello",
                    createdAt = 1L
                )
            )
        )
    }
}
