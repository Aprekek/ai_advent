package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.domain.model.ModelId
import com.aprekek.ai_advent.agentic_app.domain.model.ModelStageResult
import com.aprekek.ai_advent.agentic_app.domain.model.ModelVariant
import com.aprekek.ai_advent.agentic_app.domain.model.PricingMode
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.port.ConfigProvider
import com.aprekek.ai_advent.agentic_app.domain.usecase.BuildModelComparisonPromptUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CompareModelsWithMetricsUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GenerateSingleResponseUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import java.io.BufferedReader

class ModelMetricsComparisonController(
    private val stdinReader: BufferedReader,
    private val configProvider: ConfigProvider,
    private val mode: ChatMode,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator,
    private val compareModelsWithMetricsUseCase: CompareModelsWithMetricsUseCase,
    private val buildModelComparisonPromptUseCase: BuildModelComparisonPromptUseCase,
    private val generateSingleResponseUseCase: GenerateSingleResponseUseCase
) {
    suspend fun run(): Boolean {
        if (!configProvider.hasHuggingFaceApiKey()) {
            consoleView.printMessageBlock(
                ErrorPrefix,
                "HUGGINGFACE_API_KEY is required for this mode. Set it and try again."
            )
            return false
        }

        println("${mode.displayName} enabled.")
        println("No history is used in this mode.")
        println("Enter one prompt. The mode will run 3 model stages and return to mode selection.")
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
                    val stages = listOf(
                        ModelVariant(
                            title = "Стадия 1: deepseek v3.2-reasoner",
                            provider = ProviderType.DeepSeek,
                            modelId = ModelId("deepseek-reasoner"),
                            pricingMode = PricingMode.DeepSeekReasonerPricing
                        ),
                        ModelVariant(
                            title = "Стадия 2: deepseek v3.0",
                            provider = ProviderType.HuggingFace,
                            modelId = ModelId(configProvider.huggingFaceModelV30()),
                            pricingMode = PricingMode.NotAvailable
                        ),
                        ModelVariant(
                            title = "Стадия 3: meta-llama/Llama-3.1-8B-Instruct",
                            provider = ProviderType.HuggingFace,
                            modelId = ModelId("meta-llama/Llama-3.1-8B-Instruct"),
                            pricingMode = PricingMode.NotAvailable
                        )
                    )

                    val stageOutputs = mutableListOf<ModelStageResult>()
                    for (stage in stages) {
                        println()
                        consoleView.printSectionSeparator()
                        println(stage.title)
                        val stageResult = loadingIndicator.withLoadingIndicator {
                            compareModelsWithMetricsUseCase(parsedInput.text, listOf(stage))
                        }
                        val output = stageResult.getOrElse { error ->
                            consoleView.printMessageBlock(ErrorPrefix, error.message ?: "Unknown error")
                            return false
                        }.first()

                        stageOutputs += output
                        println()
                        consoleView.printMessageBlock(AssistantPrefix, output.response)
                        consoleView.printModelMetrics(output.metrics, output.pricingMode)
                    }

                    println()
                    consoleView.printSectionSeparator()
                    println("Сравнение: качество, скорость, ресурсоёмкость")
                    val analysisPrompt = buildModelComparisonPromptUseCase(parsedInput.text, stageOutputs)
                    val analysisResult = loadingIndicator.withLoadingIndicator {
                        generateSingleResponseUseCase(analysisPrompt)
                    }
                    analysisResult.onSuccess { analysis ->
                        consoleView.printMessageBlock(AssistantPrefix, analysis)
                    }.onFailure { exception ->
                        consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                    }

                    println()
                    println("Different models metrics compare completed. Returning to mode selection...")
                    return false
                }
            }
        }
    }
}
