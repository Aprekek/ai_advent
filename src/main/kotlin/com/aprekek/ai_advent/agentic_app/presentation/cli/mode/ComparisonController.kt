package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import com.aprekek.ai_advent.agentic_app.presentation.cli.SingleRequestExecutor
import java.io.BufferedReader

class ComparisonController(
    private val stdinReader: BufferedReader,
    private val config: AppConfig,
    private val mode: ChatMode,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator,
    private val requestExecutor: SingleRequestExecutor
) {
    suspend fun run(): Boolean {
        val comparisonModeOptions = GenerationOptions(maxTokens = 2048)

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
                    consoleView.printHelp(config.model, config.responseLanguage, mode)
                    continue
                }

                is ParsedInput.Message -> {
                    val variants = listOf(
                        ComparisonVariant(
                            title = "Без доп. иструкций",
                            execute = { userPrompt ->
                                requestExecutor.execute(userPrompt, options = comparisonModeOptions).map { response ->
                                    ComparisonOutput(response = response)
                                }
                            }
                        ),
                        ComparisonVariant(
                            title = "Пошаговое решение",
                            execute = { userPrompt ->
                                requestExecutor.execute(
                                    "$userPrompt\n\nУсловие: реши задачу пошагово.",
                                    options = comparisonModeOptions
                                ).map { response ->
                                    ComparisonOutput(response = response)
                                }
                            }
                        ),
                        ComparisonVariant(
                            title = "Генерация промпта",
                            execute = { userPrompt ->
                                val generatedPromptResult = requestExecutor.execute(
                                    "Сгенерируй промпт для решения задачи пользователя. Верни только текст промпта без пояснений.\n\nЗадача пользователя:\n$userPrompt",
                                    options = comparisonModeOptions
                                )

                                generatedPromptResult.mapCatching { generatedPrompt ->
                                    ComparisonOutput(
                                        generatedPrompt = generatedPrompt,
                                        response = requestExecutor.execute(
                                            generatedPrompt,
                                            options = comparisonModeOptions
                                        ).getOrThrow()
                                    )
                                }
                            }
                        ),
                        ComparisonVariant(
                            title = "Группа экспертов (Аналитик, Инженер, Критик)",
                            execute = { userPrompt ->
                                requestExecutor.execute(
                                    "$userPrompt\n\nУсловие: ответь с точки зрения трех экспертов: Аналитика, Инженера, Критика.",
                                    options = comparisonModeOptions
                                ).map { response ->
                                    ComparisonOutput(response = response)
                                }
                            }
                        )
                    )

                    for ((index, variant) in variants.withIndex()) {
                        if (index > 0) {
                            val shouldExit = consoleView.waitForEnterToContinue(stdinReader)
                            if (shouldExit) {
                                return true
                            }
                        }

                        println()
                        consoleView.printSectionSeparator()
                        println("Вариант: ${variant.title}")
                        val result = loadingIndicator.withLoadingIndicator {
                            variant.execute(parsedInput.text)
                        }
                        result.onSuccess { output ->
                            output.generatedPrompt?.let { generatedPrompt ->
                                println("Сгенерированный промпт:")
                                consoleView.printMessageBlock("", generatedPrompt)
                            }
                            consoleView.printMessageBlock(AssistantPrefix, output.response)
                        }.onFailure { exception ->
                            consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                        }
                    }

                    println()
                    println("Comparison mode completed. Returning to mode selection...")
                    return false
                }
            }
        }
    }
}

private data class ComparisonVariant(
    val title: String,
    val execute: suspend (String) -> Result<ComparisonOutput>
)

private data class ComparisonOutput(
    val response: String,
    val generatedPrompt: String? = null
)
