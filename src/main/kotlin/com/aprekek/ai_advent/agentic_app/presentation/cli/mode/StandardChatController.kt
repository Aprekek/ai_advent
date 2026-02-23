package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import java.io.BufferedReader
import java.util.UUID

class StandardChatController(
    private val stdinReader: BufferedReader,
    private val config: AppConfig,
    private val mode: ChatMode,
    private val sendMessageUseCase: SendMessageUseCase,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator
) {
    private val sessionId = UUID.randomUUID().toString()

    suspend fun run(): Boolean {
        println("${mode.displayName} enabled.")
        println("Type your message and press Enter.")
        println("Commands: /help, q (back to mode selection), /exit")

        while (true) {
            consoleView.printPrompt()

            when (val parsedInput = commandParser.parse(stdinReader.readLine())) {
                ParsedInput.Blank -> continue
                ParsedInput.Exit -> return true
                ParsedInput.Back -> {
                    println("Returning to mode selection...")
                    return false
                }

                ParsedInput.Help -> {
                    consoleView.printHelp(config.model, config.responseLanguage, mode)
                    continue
                }

                is ParsedInput.Message -> {
                    val result = loadingIndicator.withLoadingIndicator {
                        sendMessageUseCase(
                            sessionId = sessionId,
                            rawInput = parsedInput.text,
                            options = mode.requestOptions
                        )
                    }
                    result.onSuccess { response ->
                        consoleView.printMessageBlock(AssistantPrefix, response)
                    }.onFailure { exception ->
                        consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                    }
                }
            }
        }
    }
}
