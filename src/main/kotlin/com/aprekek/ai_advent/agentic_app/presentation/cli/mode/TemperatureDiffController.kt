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
import com.aprekek.ai_advent.agentic_app.presentation.cli.SectionSeparator
import com.aprekek.ai_advent.agentic_app.presentation.cli.SingleRequestExecutor
import java.io.BufferedReader

class TemperatureDiffController(
    private val stdinReader: BufferedReader,
    private val config: AppConfig,
    private val mode: ChatMode,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator,
    private val requestExecutor: SingleRequestExecutor
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
                    consoleView.printHelp(config.model, config.responseLanguage, mode)
                    continue
                }

                is ParsedInput.Message -> {
                    val variants = listOf(
                        TemperatureVariant(
                            title = "Температура: 0 (точность)",
                            execute = { userPrompt ->
                                requestExecutor.execute(
                                    userPrompt,
                                    options = GenerationOptions(temperature = 0.0)
                                ).map { response ->
                                    TemperatureOutput(response = response)
                                }
                            }
                        ),
                        TemperatureVariant(
                            title = "Температура: 1 (баланс)",
                            execute = { userPrompt ->
                                requestExecutor.execute(
                                    userPrompt,
                                    options = GenerationOptions(temperature = 1.0)
                                ).map { response ->
                                    TemperatureOutput(response = response)
                                }
                            }
                        ),
                        TemperatureVariant(
                            title = "Температура: 2 (креатив)",
                            execute = { userPrompt ->
                                requestExecutor.execute(
                                    userPrompt,
                                    options = GenerationOptions(temperature = 2.0)
                                ).map { response ->
                                    TemperatureOutput(response = response)
                                }
                            }
                        )
                    )

                    val variantResponses = mutableListOf<Pair<String, String>>()
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
                        val result = loadingIndicator.withLoadingIndicator {
                            variant.execute(parsedInput.text)
                        }
                        result.onSuccess { output ->
                            variantResponses += variant.title to output.response
                            consoleView.printMessageBlock(AssistantPrefix, output.response)
                        }.onFailure { exception ->
                            consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                        }
                    }

                    if (variantResponses.size == variants.size) {
                        val comparisonPrompt = buildTemperatureComparisonPrompt(parsedInput.text, variantResponses)
                        println()
                        println(SectionSeparator)
                        println("Сравнение ответов (точность и креативность)")
                        val comparisonResult = loadingIndicator.withLoadingIndicator {
                            requestExecutor.execute(
                                comparisonPrompt,
                                options = GenerationOptions(temperature = 0.0, maxTokens = 2048)
                            )
                        }
                        comparisonResult.onSuccess { response ->
                            consoleView.printMessageBlock(AssistantPrefix, response)
                        }.onFailure { exception ->
                            consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                        }
                    } else {
                        println()
                        println("Comparison step skipped: at least one variant failed.")
                    }

                    println()
                    println("Temperature diff mode completed. Returning to mode selection...")
                    return false
                }
            }
        }
    }

    private fun buildTemperatureComparisonPrompt(
        userPrompt: String,
        responses: List<Pair<String, String>>
    ): String {
        val responsesBlock = responses.joinToString(separator = "\n\n") { (title, response) ->
            "$title:\n$response"
        }
        return """
            Пользовательская задача:
            $userPrompt
            
            Ниже три ответа модели с разной температурой:
            $responsesBlock
            
            Сравни их по двум критериям:
            1) точность ответа,
            2) креативность.
            
            Дай краткую структурированную оценку по каждому варианту и сделай итоговый вывод:
            какой вариант лучший по точности, какой лучший по креативности, и какой оптимален по балансу.
        """.trimIndent()
    }
}

private data class TemperatureVariant(
    val title: String,
    val execute: suspend (String) -> Result<TemperatureOutput>
)

private data class TemperatureOutput(
    val response: String
)
