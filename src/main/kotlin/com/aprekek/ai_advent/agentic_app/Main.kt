package com.aprekek.ai_advent.agentic_app

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekChatRepository
import com.aprekek.ai_advent.agentic_app.domain.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.ChatRequestOptions
import com.aprekek.ai_advent.agentic_app.domain.ChatRole
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
            if (mode == ChatMode.Comparison) {
                val shouldExit = runComparisonMode(stdinReader, chatRepository, config, mode)
                if (shouldExit) {
                    return@runBlocking
                }
                println()
                continue
            }

            val sendMessageUseCase = SendMessageUseCase(chatRepository)
            val shouldExit = runChatMode(stdinReader, sendMessageUseCase, config, mode)
            if (shouldExit) {
                return@runBlocking
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
        println("1 - Standart mode")
        println("2 - Short mode (max tokens: 512, one paragraph, stop on empty line)")
        println("3 - Comparison mode")
        println("q - Exit")
        print("> ")
        System.out.flush()

        when (val option = stdinReader.readLine()?.trim()?.lowercase()) {
            "1" -> return ChatMode.Standard
            "2" -> return ChatMode.Short
            "3" -> return ChatMode.Comparison
            "q" -> return null
            else -> println("Unknown option $option. Please choose 1, 2, 3 or q.")
        }
    }
}

private suspend fun runChatMode(
    stdinReader: BufferedReader,
    sendMessageUseCase: SendMessageUseCase,
    config: AppConfig,
    mode: ChatMode
): Boolean {
    println("${mode.displayName} enabled.")
    println("Type your message and press Enter.")
    println("Commands: /help, q (back to mode selection), /exit")

    while (true) {
        print("you> ")
        System.out.flush()

        val rawInput = stdinReader.readLine() ?: return true
        val input = rawInput.trim()

        if (input.isBlank()) {
            continue
        }

        if (input.equals("/exit", ignoreCase = true)) {
            return true
        }

        if (input.equals("q", ignoreCase = true)) {
            println("Returning to mode selection...")
            return false
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
}

private suspend fun runComparisonMode(
    stdinReader: BufferedReader,
    chatRepository: DeepSeekChatRepository,
    config: AppConfig,
    mode: ChatMode
): Boolean {
    println("${mode.displayName} enabled.")
    println("No history is used in this mode.")
    println("Enter one prompt to run all variants.")
    println("Commands: /help, q (back to mode selection), /exit")

    while (true) {
        print("you> ")
        System.out.flush()

        val rawInput = stdinReader.readLine() ?: return true
        val input = rawInput.trim()

        if (input.isBlank()) {
            continue
        }

        if (input.equals("/exit", ignoreCase = true)) {
            return true
        }

        if (input.equals("q", ignoreCase = true)) {
            println("Returning to mode selection...")
            return false
        }

        if (input == "/help") {
            printHelp(config.model, config.responseLanguage, mode)
            continue
        }

        val variants = listOf(
            ComparisonVariant(
                title = "Без доп. иструкций",
                execute = { userPrompt ->
                    requestOnce(chatRepository, userPrompt)
                }
            ),
            ComparisonVariant(
                title = "Пошаговое решение",
                execute = { userPrompt ->
                    requestOnce(
                        chatRepository,
                        "$userPrompt\n\nУсловие: реши задачу пошагово."
                    )
                }
            ),
            ComparisonVariant(
                title = "Генерация промпта",
                execute = { userPrompt ->
                    val generatedPromptResult = requestOnce(
                        chatRepository,
                        "Сгенерируй промпт для решения задачи пользователя. Верни только текст промпта без пояснений.\n\nЗадача пользователя:\n$userPrompt"
                    )

                    generatedPromptResult.onSuccess { generatedPrompt ->
                        println("Сгенерированный промпт:")
                        println(generatedPrompt)
                    }

                    generatedPromptResult.mapCatching { generatedPrompt ->
                        requestOnce(chatRepository, generatedPrompt).getOrThrow()
                    }
                }
            ),
            ComparisonVariant(
                title = "Группа экспертов (Аналитик, Инженер, Критик)",
                execute = { userPrompt ->
                    requestOnce(
                        chatRepository,
                        "$userPrompt\n\nУсловие: ответь с точки зрения трех экспертов: Аналитика, Инженера, Критика."
                    )
                }
            )
        )

        for (variant in variants) {
            println()
            println("Вариант: ${variant.title}")
            val result = withLoadingIndicator {
                variant.execute(input)
            }
            result.onSuccess { response ->
                println("assistant> $response")
            }.onFailure { exception ->
                println("error> ${exception.message ?: "Unknown error"}")
            }
        }

        println()
        println("Comparison mode completed. Returning to mode selection...")
        return false
    }
}

private suspend fun requestOnce(
    chatRepository: DeepSeekChatRepository,
    prompt: String,
    options: ChatRequestOptions = ChatRequestOptions.Standard
): Result<String> {
    val input = prompt.trim()
    if (input.isBlank()) {
        return Result.failure(IllegalArgumentException("Input must not be blank"))
    }
    return runCatching {
        chatRepository.sendMessage(
            messages = listOf(ChatMessage(role = ChatRole.User, content = input)),
            options = options
        ).trim()
    }.mapCatching { output ->
        if (output.isBlank()) {
            throw IllegalStateException("DeepSeek returned an empty response")
        }
        output
    }
}

private fun printHelp(model: String, responseLanguage: String, mode: ChatMode) {
    println("Current model: $model")
    println("Response language: $responseLanguage")
    println("Current mode: ${mode.displayName}")
    println("Use q to return to mode selection (history resets after mode switch).")
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
    Standard(
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
    ),
    Comparison(
        displayName = "Comparison mode",
        requestOptions = ChatRequestOptions.Standard
    )
}

private data class ComparisonVariant(
    val title: String,
    val execute: suspend (String) -> Result<String>
)
