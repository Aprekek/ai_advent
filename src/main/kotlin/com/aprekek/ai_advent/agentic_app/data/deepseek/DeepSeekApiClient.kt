package com.aprekek.ai_advent.agentic_app.data.deepseek

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
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
    @Volatile
    var lastCallMetrics: CallMetrics? = null
        private set

    private val json = Json {
        ignoreUnknownKeys = true
    }

    suspend fun sendMessages(
        messages: List<DeepSeekMessage>,
        options: GenerationOptions = GenerationOptions.Standard,
        requestContext: ProviderRequestContext = defaultRequestContext()
    ): String {
        val startedAtMs = System.currentTimeMillis()
        val response = httpClient.post(chatCompletionUrl(requestContext.baseUrl)) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            accept(ContentType.Application.Json)
            bearerAuth(requestContext.apiKey)
            setBody(
                DeepSeekChatCompletionRequest(
                    model = requestContext.model,
                    messages = listOf(
                        DeepSeekMessage(
                            role = "system",
                            content = systemInstruction(config.responseLanguage, options.extraInstruction)
                        )
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
        val elapsedMs = System.currentTimeMillis() - startedAtMs
        lastCallMetrics = CallMetrics(
            responseTimeMs = elapsedMs,
            promptTokens = payload.usage?.promptTokens,
            completionTokens = payload.usage?.completionTokens,
            totalTokens = payload.usage?.totalTokens
        )
        return payload.choices.firstOrNull()?.message?.content?.trim().orEmpty()
    }

    private fun defaultRequestContext(): ProviderRequestContext = ProviderRequestContext(
        model = config.model,
        apiKey = config.apiKey,
        baseUrl = config.baseUrl
    )

    private fun chatCompletionUrl(baseUrl: String): String = "${baseUrl.trimEnd('/')}/chat/completions"

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

data class CallMetrics(
    val responseTimeMs: Long,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?
)
