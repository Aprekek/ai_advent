package com.aprekek.ai_advent.agentic_app.data.provider.huggingface

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders

class HuggingFaceApiClient(
    private val httpClient: HttpClient
) {
    @Volatile
    var lastCallMetrics: HuggingFaceCallMetrics? = null
        private set

    suspend fun sendChatCompletion(
        request: HuggingFaceChatCompletionRequest,
        apiKey: String,
        baseUrl: String
    ): HuggingFaceChatCompletionResponse {
        val startedAtMs = System.currentTimeMillis()
        val response = httpClient.post("${baseUrl.trimEnd('/')}/chat/completions") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(request)
        }
        val payload = response.body<HuggingFaceChatCompletionResponse>()
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        lastCallMetrics = HuggingFaceCallMetrics(
            responseTimeMs = elapsedMs,
            promptTokens = payload.usage?.promptTokens,
            completionTokens = payload.usage?.completionTokens,
            totalTokens = payload.usage?.totalTokens
        )
        return payload
    }
}

data class HuggingFaceCallMetrics(
    val responseTimeMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
