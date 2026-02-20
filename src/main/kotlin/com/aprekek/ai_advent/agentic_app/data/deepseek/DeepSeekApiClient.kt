package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.ChatRequestOptions
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
    private val httpClient: HttpClient,
    private val config: AppConfig
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun sendMessages(
        messages: List<DeepSeekMessage>,
        options: ChatRequestOptions = ChatRequestOptions.Standard
    ): String {
        val apiKey = options.apiKeyOverride?.trim().orEmpty().ifBlank { config.apiKey }
        val response = httpClient.post(chatCompletionUrl(options.baseUrlOverride)) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(apiKey)
            setBody(
                DeepSeekChatCompletionRequest(
                    model = options.modelOverride?.trim().orEmpty().ifBlank { config.model },
                    messages = listOf(
                        DeepSeekMessage(
                            role = "system",
                            content = systemInstruction(config.responseLanguage, options.extraSystemInstruction)
                        ),
                    ) + messages,
                    maxTokens = options.maxTokens,
                    stop = options.stopSequences.takeIf { it.isNotEmpty() },
                    temperature = options.temperature
                )
            )
        }

        if (!response.status.isSuccess()) {
            throw IllegalStateException(buildErrorMessage(response))
        }

        val payload = response.body<DeepSeekChatCompletionResponse>()
        return payload.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    private fun chatCompletionUrl(baseUrlOverride: String?): String {
        val baseUrl = baseUrlOverride?.trim().orEmpty().ifBlank { config.baseUrl }
        return "${baseUrl.trimEnd('/')}/chat/completions"
    }

    private fun systemInstruction(language: String, extraInstruction: String?): String {
        val baseInstruction = "Always respond in $language unless the user explicitly requests another language."
        val trimmedExtraInstruction = extraInstruction?.trim().orEmpty()
        return if (trimmedExtraInstruction.isEmpty()) {
            baseInstruction
        } else {
            "$baseInstruction $trimmedExtraInstruction"
        }
    }

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
