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

private const val MaxConsoleLineLength = 160
private const val UserPrompt = "you> "
private const val AssistantPrefix = "assistant> "
private const val ErrorPrefix = "error> "
private const val SectionSeparator = "------------------------------------------------------------"

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
        printPrompt()

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
            printMessageBlock(AssistantPrefix, response)
        }.onFailure { exception ->
            printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
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
        printPrompt()

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
                    requestOnce(chatRepository, userPrompt).map { response ->
                        ComparisonOutput(response = response)
                    }
                }
            ),
            ComparisonVariant(
                title = "Пошаговое решение",
                execute = { userPrompt ->
                    requestOnce(
                        chatRepository,
                        "$userPrompt\n\nУсловие: реши задачу пошагово."
                    ).map { response ->
                        ComparisonOutput(response = response)
                    }
                }
            ),
            ComparisonVariant(
                title = "Генерация промпта",
                execute = { userPrompt ->
                    val generatedPromptResult = requestOnce(
                        chatRepository,
                        "Сгенерируй промпт для решения задачи пользователя. Верни только текст промпта без пояснений.\n\nЗадача пользователя:\n$userPrompt"
                    )

                    generatedPromptResult.mapCatching { generatedPrompt ->
                        ComparisonOutput(
                            generatedPrompt = generatedPrompt,
                            response = requestOnce(chatRepository, generatedPrompt).getOrThrow()
                        )
                    }
                }
            ),
            ComparisonVariant(
                title = "Группа экспертов (Аналитик, Инженер, Критик)",
                execute = { userPrompt ->
                    requestOnce(
                        chatRepository,
                        "$userPrompt\n\nУсловие: ответь с точки зрения трех экспертов: Аналитика, Инженера, Критика."
                    ).map { response ->
                        ComparisonOutput(response = response)
                    }
                }
            )
        )

        for ((index, variant) in variants.withIndex()) {
            if (index > 0) {
                val shouldExit = waitForEnterToContinue(stdinReader)
                if (shouldExit) {
                    return true
                }
            }

            println()
            println(SectionSeparator)
            println("Вариант: ${variant.title}")
            val result = withLoadingIndicator {
                variant.execute(input)
            }
            result.onSuccess { output ->
                output.generatedPrompt?.let { generatedPrompt ->
                    println("Сгенерированный промпт:")
                    printWrappedResponse("", generatedPrompt)
                    println()
                }
                printMessageBlock(AssistantPrefix, output.response)
            }.onFailure { exception ->
                printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
            }
        }

        println()
        println("Comparison mode completed. Returning to mode selection...")
        return false
    }
}

private fun waitForEnterToContinue(stdinReader: BufferedReader): Boolean {
    println()
    println("Нажмите Enter для перехода к следующему варианту (или /exit для выхода):")
    print("> ")
    System.out.flush()

    val input = stdinReader.readLine() ?: return true
    return input.trim().equals("/exit", ignoreCase = true)
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

private fun printPrompt() {
    println()
    print(UserPrompt)
    System.out.flush()
}

private fun printMessageBlock(prefix: String, text: String) {
    println()
    printWrappedResponse(prefix, text)
    println()
}

private fun printWrappedResponse(prefix: String, text: String) {
    val content = text.trim()
    if (content.isEmpty()) {
        if (prefix.isNotEmpty()) {
            println(prefix)
        }
        return
    }

    val availableForFirstLine = (MaxConsoleLineLength - prefix.length).coerceAtLeast(1)
    val availableForNextLines = MaxConsoleLineLength.coerceAtLeast(1)
    var prefixPrinted = prefix.isEmpty()

    val paragraphs = content.replace("\r\n", "\n").split('\n')
    paragraphs.forEach { paragraph ->
        if (paragraph.isBlank()) {
            println()
            return@forEach
        }

        var remaining = paragraph.trim()
        var firstChunkInParagraph = true
        while (remaining.isNotEmpty()) {
            val available = if (!prefixPrinted && firstChunkInParagraph) {
                availableForFirstLine
            } else {
                availableForNextLines
            }
            if (remaining.length <= available) {
                val linePrefix = if (!prefixPrinted && firstChunkInParagraph) prefix else ""
                println(linePrefix + remaining)
                prefixPrinted = true
                break
            }

            val splitAt = remaining
                .lastIndexOf(' ', startIndex = available)
                .takeIf { it > 0 }
                ?: available

            val chunk = remaining.substring(0, splitAt).trimEnd()
            val linePrefix = if (!prefixPrinted && firstChunkInParagraph) prefix else ""
            println(linePrefix + chunk)
            remaining = remaining.substring(splitAt).trimStart()
            prefixPrinted = true
            firstChunkInParagraph = false
        }
    }
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
    val execute: suspend (String) -> Result<ComparisonOutput>
)

private data class ComparisonOutput(
    val response: String,
    val generatedPrompt: String? = null
)
