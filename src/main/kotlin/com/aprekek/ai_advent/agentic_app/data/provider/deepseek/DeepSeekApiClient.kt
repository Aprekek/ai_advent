package com.aprekek.ai_advent.agentic_app.data.provider.deepseek

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class DeepSeekApiClient(
    private val httpClient: HttpClient
) {
    @Volatile
    var lastCallMetrics: CallMetrics? = null
        private set

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun sendChatCompletion(
        request: DeepSeekChatCompletionRequest,
        requestContext: ProviderRequestContext
    ): DeepSeekChatCompletionResponse {
        val startedAtMs = System.currentTimeMillis()
        val response = httpClient.post(chatCompletionUrl(requestContext.baseUrl)) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(requestContext.apiKey)
            setBody(request)
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException(buildErrorMessage(response))
        }

        val payload = response.body<DeepSeekChatCompletionResponse>()
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        lastCallMetrics = CallMetrics(
            responseTimeMs = elapsedMs,
            promptTokens = payload.usage?.promptTokens,
            completionTokens = payload.usage?.completionTokens,
            totalTokens = payload.usage?.totalTokens
        )
        return payload
    }

    private fun chatCompletionUrl(baseUrl: String): String = "${baseUrl.trimEnd('/')}/chat/completions"

    private suspend fun buildErrorMessage(response: HttpResponse): String {
        val rawBody = response.bodyAsText()
        val parsedError = runCatching { json.decodeFromString<DeepSeekErrorResponse>(rawBody) }.getOrNull()
        val apiMessage = parsedError?.error?.message?.trim().orEmpty()

        if (apiMessage.isNotEmpty()) {
            return "DeepSeek API request failed with HTTP ${response.status.value}: $apiMessage"
        }

        if (response.status == HttpStatusCode.Unauthorized) {
            return "DeepSeek API request failed with HTTP 401: check DEEPSEEK_API_KEY"
        }

        return "DeepSeek API request failed with HTTP ${response.status.value}"
    }
}

data class CallMetrics(
    val responseTimeMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
