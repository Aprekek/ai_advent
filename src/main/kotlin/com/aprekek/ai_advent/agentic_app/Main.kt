package com.aprekek.ai_advent.agentic_app

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekChatRepository
import com.aprekek.ai_advent.agentic_app.domain.ChatRequestOptions
import com.aprekek.ai_advent.agentic_app.domain.SendMessageUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    clearTerminal()

    val stdinReader = BufferedReader(InputStreamReader(System.`in`))
    val config = runCatching { AppConfig.fromEnvironment() }.getOrElse { error ->
        println("Configuration error: ${error.message}")
        return@runBlocking
    }

    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    val chatRepository = DeepSeekChatRepository(DeepSeekApiClient(httpClient, config))
    println("DeepSeek CLI started.")

    try {
        while (true) {
            val mode = selectMode(stdinReader) ?: break
            val sendMessageUseCase = SendMessageUseCase(chatRepository)

            println("${mode.displayName} enabled.")
            println("Type your message and press Enter.")
            println("Commands: /help, q (back to mode selection), /exit")

            while (true) {
                print("you> ")
                System.out.flush()

                val rawInput = stdinReader.readLine() ?: return@runBlocking
                val input = rawInput.trim()

                if (input.isBlank()) {
                    continue
                }

                if (input.equals("/exit", ignoreCase = true)) {
                    return@runBlocking
                }

                if (input.equals("q", ignoreCase = true)) {
                    println("Returning to mode selection...")
                    break
                }

                if (input == "/help") {
                    printHelp(config.model, config.responseLanguage, mode)
                    continue
                }

                val result = withLoadingIndicator {
                    sendMessageUseCase(input, mode.requestOptions)
                }
                result.onSuccess { response ->
                    println("assistant> $response")
                }.onFailure { exception ->
                    println("error> ${exception.message ?: "Unknown error"}")
                }
            }

            println()
        }
    } finally {
        httpClient.close()
    }
}

private fun clearTerminal() {
    print("\u001B[H\u001B[2J")
    System.out.flush()
}

private fun selectMode(stdinReader: BufferedReader): ChatMode? {
    while (true) {
        println("Select mode:")
        println("1 - Standard mode")
        println("2 - Short mode (max tokens: 512, one paragraph, stop on empty line)")
        println("q - Exit")
        print("> ")
        System.out.flush()

        when (val option = stdinReader.readLine()?.trim()?.lowercase()) {
            "1" -> return ChatMode.Standart
            "2" -> return ChatMode.Short
            "q" -> return null
            else -> println("Unknown option $option. Please choose 1, 2 or q.")
        }
    }
}

private fun printHelp(model: String, responseLanguage: String, mode: ChatMode) {
    println("Current model: $model")
    println("Response language: $responseLanguage")
    println("Current mode: ${mode.displayName}")
    println("Use q to return to mode selection.")
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

private enum class ChatMode(
    val displayName: String,
    val requestOptions: ChatRequestOptions
) {
    Standart(
        displayName = "Standart mode",
        requestOptions = ChatRequestOptions.Standard
    ),
    Short(
        displayName = "Short mode",
        requestOptions = ChatRequestOptions(
            maxTokens = 512,
            stopSequences = listOf("\n\n"),
            extraSystemInstruction = "Answer in one paragraph only."
        )
    )
}
