package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.app.AppConfig
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class DeepSeekApiClient(
    private val httpClient: HttpClient,
    private val config: AppConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun sendPrompt(userInput: String): String {
        val response = httpClient.post(chatCompletionUrl()) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(config.apiKey)
            setBody(
                DeepSeekChatCompletionRequest(
                    model = config.model,
                    messages = listOf(
                        DeepSeekMessage(role = "system", content = systemInstruction(config.responseLanguage)),
                        DeepSeekMessage(role = "user", content = userInput)
                    )
                )
            )
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException(buildErrorMessage(response))
        }

        val payload = response.body<DeepSeekChatCompletionResponse>()
        return payload.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    private fun chatCompletionUrl(): String = "${config.baseUrl.trimEnd('/')}/chat/completions"

    private fun systemInstruction(language: String): String =
        "Always respond in $language unless the user explicitly requests another language."

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
