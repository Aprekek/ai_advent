package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.port.MetricsProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.AddConversationUsageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.ClearConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.ClearConversationUsageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GetConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GetConversationUsageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendCompressedMessageUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.DeepSeekInputCostCacheHitPer1M
import com.aprekek.ai_advent.agentic_app.presentation.cli.DeepSeekInputCostCacheMissPer1M
import com.aprekek.ai_advent.agentic_app.presentation.cli.DeepSeekOutputCostPer1M
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import java.io.BufferedReader
import java.util.*

private const val StandardSessionId = "standard-mode-session"
private val HistoryCommandRegex = Regex("^/history\\s+(\\d+)$")

private enum class StandardContextMode {
    NoCompression,
    Compressed
}

class StandardChatController(
    private val stdinReader: BufferedReader,
    private val configProvider: ConfigProvider,
    private val mode: ChatMode,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendCompressedMessageUseCase: SendCompressedMessageUseCase,
    private val getConversationHistoryUseCase: GetConversationHistoryUseCase,
    private val clearConversationHistoryUseCase: ClearConversationHistoryUseCase,
    private val getConversationUsageUseCase: GetConversationUsageUseCase,
    private val addConversationUsageUseCase: AddConversationUsageUseCase,
    private val clearConversationUsageUseCase: ClearConversationUsageUseCase,
    private val metricsProvider: MetricsProvider,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator
) {
    private val sessionId = if (mode == ChatMode.Standard) StandardSessionId else UUID.randomUUID().toString()

    suspend fun run(): Boolean {
        val standardContextMode = if (mode == ChatMode.Standard) {
            val selected = selectStandardContextMode() ?: return false
            selected
        } else {
            StandardContextMode.NoCompression
        }

        println("${mode.displayName} enabled.")
        println("Type your message and press Enter.")
        val commandsLine = if (mode == ChatMode.Standard) {
            "Commands: /help, /history N, /clear, q (back to mode selection), /exit"
        } else {
            "Commands: /help, q (back to mode selection), /exit"
        }
        println(commandsLine)

        if (mode == ChatMode.Standard) {
            val usage = getConversationUsageUseCase(sessionId)
            printUsageStats(
                label = "Session totals",
                promptTokens = usage.promptTokens,
                completionTokens = usage.completionTokens,
                totalTokens = usage.totalTokens
            )
        }

        while (true) {
            consoleView.printPrompt()
            val rawInput = stdinReader.readLine()
            val trimmedInput = rawInput?.trim().orEmpty()

            if (mode == ChatMode.Standard && handleStandardCommands(trimmedInput, standardContextMode)) {
                continue
            }

            if (mode != ChatMode.Standard && (trimmedInput.startsWith("/history") || trimmedInput == "/clear")) {
                consoleView.printMessageBlock(ErrorPrefix, "This command is available only in Standard mode.")
                continue
            }

            when (val parsedInput = commandParser.parse(rawInput)) {
                ParsedInput.Blank -> continue
                ParsedInput.Exit -> return true
                ParsedInput.Back -> {
                    println("Returning to mode selection...")
                    return false
                }

                ParsedInput.Help -> {
                    consoleView.printHelp(configProvider.model(), configProvider.responseLanguage(), mode)
                    continue
                }

                is ParsedInput.Message -> {
                    val result = loadingIndicator.withLoadingIndicator {
                        when (standardContextMode) {
                            StandardContextMode.NoCompression -> sendMessageUseCase(
                                sessionId = sessionId,
                                rawInput = parsedInput.text,
                                options = mode.requestOptions
                            )

                            StandardContextMode.Compressed -> sendCompressedMessageUseCase(
                                sessionId = sessionId,
                                rawInput = parsedInput.text,
                                options = mode.requestOptions
                            )
                        }
                    }
                    result.onSuccess { response ->
                        consoleView.printMessageBlock(AssistantPrefix, response)
                        if (mode == ChatMode.Standard) {
                            printRequestAndSessionUsage()
                        }
                    }.onFailure { exception ->
                        consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                    }
                }
            }
        }
    }

    private fun handleStandardCommands(
        trimmedInput: String,
        standardContextMode: StandardContextMode
    ): Boolean {
        if (trimmedInput == "/clear") {
            clearConversationHistoryUseCase(sessionId)
            sendCompressedMessageUseCase.clearCompressedHistory(sessionId)
            clearConversationUsageUseCase(sessionId)
            consoleView.printMessageBlock(AssistantPrefix, "History cleared.")
            return true
        }

        val match = HistoryCommandRegex.matchEntire(trimmedInput)
        if (match != null) {
            val dialogsToShow = match.groupValues[1].toIntOrNull()
            if (dialogsToShow == null || dialogsToShow <= 0) {
                consoleView.printMessageBlock(ErrorPrefix, "Usage: /history <positive number>")
                return true
            }

            if (standardContextMode == StandardContextMode.NoCompression) {
                val history = getConversationHistoryUseCase(sessionId)
                val dialogs = history.chunked(2).filter { it.size == 2 }.takeLast(dialogsToShow)
                if (dialogs.isEmpty()) {
                    consoleView.printMessageBlock(AssistantPrefix, "History is empty.")
                    return true
                }

                dialogs.forEachIndexed { index, dialog ->
                    println()
                    println("Dialog ${index + 1}:")
                    consoleView.printMessageBlock("you> ", dialog[0].content)
                    consoleView.printMessageBlock(AssistantPrefix, dialog[1].content)
                }
            } else {
                val blocks = sendCompressedMessageUseCase.compressedHistory(sessionId).takeLast(dialogsToShow)
                if (blocks.isEmpty()) {
                    consoleView.printMessageBlock(AssistantPrefix, "History is empty.")
                    return true
                }

                blocks.forEachIndexed { index, block ->
                    println()
                    println("Context block ${index + 1}:")
                    val prefix = if (block.role.name.equals("User", ignoreCase = true)) "you> " else AssistantPrefix
                    consoleView.printMessageBlock(prefix, block.content)
                }
            }
            return true
        }

        if (trimmedInput.startsWith("/history")) {
            consoleView.printMessageBlock(ErrorPrefix, "Usage: /history <positive number>")
            return true
        }
        return false
    }

    private fun selectStandardContextMode(): StandardContextMode? {
        while (true) {
            println("Select Standard context mode:")
            println("1 - No compression")
            println("2 - Compression (every 5th sentence, 1 raw + up to 3 compressed)")
            println("q - Back")
            print("> ")
            System.out.flush()

            when (stdinReader.readLine()?.trim()?.lowercase()) {
                "1" -> return StandardContextMode.NoCompression
                "2" -> return StandardContextMode.Compressed
                "q" -> return null
                else -> println("Unknown option. Please choose 1, 2 or q.")
            }
        }
    }

    private fun printRequestAndSessionUsage() {
        val metrics = metricsProvider.lastMetrics(ProviderType.DeepSeek)
        if (metrics == null) {
            consoleView.printMessageBlock(ErrorPrefix, "Metrics are unavailable for this request.")
            return
        }

        val promptTokens = metrics.promptTokens ?: 0
        val completionTokens = metrics.completionTokens ?: 0
        val totalTokens = metrics.totalTokens ?: (promptTokens + completionTokens)

        addConversationUsageUseCase(
            sessionId = sessionId,
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens
        )

        val totalUsage = getConversationUsageUseCase(sessionId)
        printUsageStats("Current request", promptTokens, completionTokens, totalTokens)
        printUsageStats(
            "Session totals",
            totalUsage.promptTokens,
            totalUsage.completionTokens,
            totalUsage.totalTokens
        )
    }

    private fun printUsageStats(label: String, promptTokens: Int, completionTokens: Int, totalTokens: Int) {
        val promptMillions = promptTokens / 1_000_000.0
        val completionMillions = completionTokens / 1_000_000.0
        val inputHit = promptMillions * DeepSeekInputCostCacheHitPer1M
        val inputMiss = promptMillions * DeepSeekInputCostCacheMissPer1M
        val output = completionMillions * DeepSeekOutputCostPer1M
        val totalHit = inputHit + output
        val totalMiss = inputMiss + output
        println("$label: prompt = $promptTokens, completion = $completionTokens, total = $totalTokens")
        println("$label price (cache hit): $${"%.6f".format(totalHit)}")
        println("$label price (cache miss): $${"%.6f".format(totalMiss)}")
    }
}
