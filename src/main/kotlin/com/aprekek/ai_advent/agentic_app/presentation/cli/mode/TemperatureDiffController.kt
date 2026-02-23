package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.BuildTemperatureComparisonPromptUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CompareTemperatureUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GenerateSingleResponseUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import com.aprekek.ai_advent.agentic_app.presentation.cli.SectionSeparator
import java.io.BufferedReader

class TemperatureDiffController(
    private val stdinReader: BufferedReader,
    private val configProvider: ConfigProvider,
    private val mode: ChatMode,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator,
    private val compareTemperatureUseCase: CompareTemperatureUseCase,
    private val buildTemperatureComparisonPromptUseCase: BuildTemperatureComparisonPromptUseCase,
    private val generateSingleResponseUseCase: GenerateSingleResponseUseCase
) {
    suspend fun run(): Boolean {
        println("${mode.displayName} enabled.")
        println("No history is used in this mode.")
        println("Enter one prompt to compare answers with different temperature values.")
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
                    val variantsResult = loadingIndicator.withLoadingIndicator {
                        compareTemperatureUseCase(parsedInput.text)
                    }
                    val variants = variantsResult.getOrElse { error ->
                        consoleView.printMessageBlock(ErrorPrefix, error.message ?: "Unknown error")
                        return false
                    }

                    for ((index, variant) in variants.withIndex()) {
                        if (index > 0) {
                            val shouldExit = consoleView.waitForEnterToContinue(stdinReader)
                            if (shouldExit) {
                                return true
                            }
                        }
                        println()
                        consoleView.printSectionSeparator()
                        println(variant.title)
                        consoleView.printMessageBlock(AssistantPrefix, variant.response)
                    }

                    val comparisonPrompt = buildTemperatureComparisonPromptUseCase(parsedInput.text, variants)
                    println()
                    println(SectionSeparator)
                    println("Сравнение ответов (точность и креативность)")
                    val comparisonResult = loadingIndicator.withLoadingIndicator {
                        generateSingleResponseUseCase(
                            comparisonPrompt,
                            GenerationOptions(temperature = 0.0, maxTokens = 2048)
                        )
                    }
                    comparisonResult.onSuccess { response ->
                        consoleView.printMessageBlock(AssistantPrefix, response)
                    }.onFailure { exception ->
                        consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                    }

                    println()
                    println("Temperature diff mode completed. Returning to mode selection...")
                    return false
                }
            }
        }
    }
}
