package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.ComparePromptStrategiesUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import java.io.BufferedReader

class ComparisonController(
    private val stdinReader: BufferedReader,
    private val configProvider: ConfigProvider,
    private val mode: ChatMode,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator,
    private val comparePromptStrategiesUseCase: ComparePromptStrategiesUseCase
) {
    suspend fun run(): Boolean {
        println("${mode.displayName} enabled.")
        println("No history is used in this mode.")
        println("Enter one prompt to run all variants.")
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
                    consoleView.printHelp(configProvider.model(), configProvider.responseLanguage(), mode)
                    continue
                }

                is ParsedInput.Message -> {
                    val result = loadingIndicator.withLoadingIndicator {
                        comparePromptStrategiesUseCase(parsedInput.text)
                    }
                    val outputs = result.getOrElse { error ->
                        consoleView.printMessageBlock(ErrorPrefix, error.message ?: "Unknown error")
                        return false
                    }

                    for ((index, output) in outputs.withIndex()) {
                        if (index > 0) {
                            val shouldExit = consoleView.waitForEnterToContinue(stdinReader)
                            if (shouldExit) {
                                return true
                            }
                        }

                        println()
                        consoleView.printSectionSeparator()
                        println("Вариант: ${output.title}")
                        output.generatedPrompt?.let { generatedPrompt ->
                            println("Сгенерированный промпт:")
                            consoleView.printMessageBlock("", generatedPrompt)
                        }
                        consoleView.printMessageBlock(AssistantPrefix, output.response)
                    }

                    println()
                    println("Comparison mode completed. Returning to mode selection...")
                    return false
                }
            }
        }
    }
}
