package com.aprekek.ai_advent.agentic_app

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.cli.TerminalInput
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekChatRepository
import com.aprekek.ai_advent.agentic_app.domain.SendMessageUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val config = runCatching { AppConfig.fromEnvironment() }.getOrElse { error ->
        println("Configuration error: ${error.message}")
        return@runBlocking
    }

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    val chatRepository = DeepSeekChatRepository(DeepSeekApiClient(httpClient, config))
    val sendMessageUseCase = SendMessageUseCase(chatRepository)
    val terminalInput = TerminalInput()

    println("DeepSeek CLI started.")
    println("Type your message and press Enter.")
    println("Commands: /help, /exit")

    try {
        terminalInput
            .lines()
            .map { it.trim() }
            .takeWhile { it.lowercase() != "/exit" }
            .collect { input ->
                if (input.isBlank()) {
                    return@collect
                }
                if (input == "/help") {
                    printHelp(config.model, config.responseLanguage)
                    return@collect
                }

                val result = withLoadingIndicator {
                    sendMessageUseCase(input)
                }
                result
                    .onSuccess { response ->
                        println("assistant> $response")
                    }
                    .onFailure { exception ->
                        println("error> ${exception.message ?: "Unknown error"}")
                    }
            }
    } finally {
        httpClient.close()
    }
}

private fun printHelp(model: String, responseLanguage: String) {
    println("Current model: $model")
    println("Response language: $responseLanguage")
    println("Use /exit to stop the app.")
}

private suspend fun <T> withLoadingIndicator(block: suspend () -> T): T = withContext(Dispatchers.IO) {
    val frames = listOf("|", "/", "-", "\\")
    val spinnerJob = launch {
        var index = 0
        while (isActive) {
            print("\rassistant> thinking ${frames[index % frames.size]}")
            System.out.flush()
            index++
            delay(120)
        }
    }

    try {
        block()
    } finally {
        spinnerJob.cancel()
        print("\r                              \r")
        System.out.flush()
    }
}
