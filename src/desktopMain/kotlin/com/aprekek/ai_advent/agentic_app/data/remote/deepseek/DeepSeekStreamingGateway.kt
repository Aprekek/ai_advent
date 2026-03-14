package com.aprekek.ai_advent.agentic_app.data.remote.deepseek

import com.aprekek.ai_advent.agentic_app.domain.model.ApiError
import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.StreamEvent
import com.aprekek.ai_advent.agentic_app.domain.port.ChatStreamingGateway
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.math.pow
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json

class DeepSeekStreamingGateway(
    private val httpClient: HttpClient,
    private val json: Json,
    private val endpoint: String = DEFAULT_ENDPOINT,
    private val maxRetries: Int = 3,
    private val requestTimeoutMillis: Long = 120_000L
) : ChatStreamingGateway {
    private val parser = DeepSeekSseParser(json)

    override fun streamChat(request: ChatRequest): Flow<StreamEvent> = flow {
        emit(StreamEvent.Started)

        var attempt = 0
        var emittedAnyDelta = false

        while (true) {
            try {
                streamOnce(request) { delta ->
                    emittedAnyDelta = true
                    splitDelta(delta).forEach { part ->
                        emit(StreamEvent.Delta(part))
                        // Keep stream visibly progressive in UI with ~60 FPS cadence.
                        delay(STREAM_UI_DELAY_MS)
                        yield()
                    }
                }

                emit(StreamEvent.Completed)
                return@flow
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val mappedError = mapError(error)
                val canRetry = !emittedAnyDelta && isRetryable(mappedError) && attempt < maxRetries
                if (!canRetry) {
                    throw mappedError
                }

                attempt += 1
                emit(StreamEvent.Reconnecting(attempt))
                delay(backoffWithJitter(attempt))
            }
        }
    }

    private suspend fun streamOnce(
        request: ChatRequest,
        onDelta: suspend (String) -> Unit
    ) {
        withTimeout(requestTimeoutMillis) {
            val response = httpClient.post(endpoint) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Text.EventStream)
                bearerAuth(request.apiKey)
                setBody(
                    DeepSeekStreamRequest(
                        model = request.model,
                        temperature = request.temperature,
                        maxTokens = request.maxTokens,
                        messages = request.messages.map { message ->
                            DeepSeekMessage(
                                role = message.role.name.lowercase(),
                                content = message.content
                            )
                        }
                    )
                )
            }

            if (!response.status.isSuccess()) {
                throw buildApiException(status = response.status, rawBody = response.bodyAsText())
            }

            val channel = response.bodyAsChannel()
            var completed = false

            while (!channel.isClosedForRead) {
                val line = channel.readUTF8Line()
                if (line == null) {
                    break
                }

                if (line.isBlank()) {
                    continue
                }

                if (!line.startsWith("data:")) {
                    continue
                }

                val payload = line.removePrefix("data:").trimStart()
                when (val parsed = parser.parseDataPayload(payload)) {
                    is ParsedSsePayload.Delta -> {
                        if (parsed.content.isNotEmpty()) {
                            onDelta(parsed.content)
                        }
                    }
                    ParsedSsePayload.Done -> {
                        completed = true
                        break
                    }
                }
            }

            if (!completed) {
                throw IOException("SSE stream closed before completion token")
            }
        }
    }

    private fun buildApiException(status: HttpStatusCode, rawBody: String): DeepSeekApiException {
        val parsedMessage = runCatching {
            json.decodeFromString(DeepSeekErrorEnvelope.serializer(), rawBody)
                .error
                ?.message
                ?.trim()
        }.getOrNull().orEmpty()

        val message = if (parsedMessage.isNotEmpty()) {
            parsedMessage
        } else {
            "DeepSeek API error ${status.value}"
        }

        return DeepSeekApiException(
            statusCode = status.value,
            message = message
        )
    }

    private fun mapError(error: Throwable): Throwable {
        if (error is ApiError) return error

        return when (error) {
            is DeepSeekApiException -> {
                when (error.statusCode) {
                    HttpStatusCode.Unauthorized.value -> ApiError.Unauthorized("DeepSeek API вернул 401")
                    HttpStatusCode.TooManyRequests.value -> ApiError.RateLimited("DeepSeek API вернул 429")
                    in 500..599 -> ApiError.Server("DeepSeek API недоступен (${error.statusCode})")
                    else -> ApiError.Unknown(error.message ?: "DeepSeek API error")
                }
            }
            is TimeoutCancellationException,
            is SocketTimeoutException -> ApiError.Timeout()
            is IOException -> ApiError.Network(error.message ?: "Сетевая ошибка")
            else -> ApiError.Unknown(error.message ?: "Неизвестная ошибка")
        }
    }

    private fun isRetryable(error: Throwable): Boolean {
        return error is ApiError.Network || error is ApiError.Timeout || error is ApiError.RateLimited || error is ApiError.Server
    }

    private fun backoffWithJitter(attempt: Int): Long {
        val base = 500L
        val exponential = (base * 2.0.pow((attempt - 1).toDouble())).toLong()
        val jitter = Random.nextLong(0L, 250L)
        return exponential + jitter
    }

    private fun splitDelta(value: String): List<String> {
        if (value.length <= DELTA_CHUNK_SIZE) return listOf(value)
        val chunks = mutableListOf<String>()
        var start = 0
        while (start < value.length) {
            val end = (start + DELTA_CHUNK_SIZE).coerceAtMost(value.length)
            chunks += value.substring(start, end)
            start = end
        }
        return chunks
    }

    companion object {
        private const val DEFAULT_ENDPOINT = "https://api.deepseek.com/v1/chat/completions"
        private const val DELTA_CHUNK_SIZE = 12
        private const val STREAM_UI_DELAY_MS = 16L
    }
}
