package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.domain.model.ModelStageResult
import com.aprekek.ai_advent.agentic_app.domain.model.ModelVariant
import com.aprekek.ai_advent.agentic_app.domain.model.PricingMode
import com.aprekek.ai_advent.agentic_app.domain.model.ProviderType
import com.aprekek.ai_advent.agentic_app.domain.usecase.CompareModelsWithMetricsUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.ErrorPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ParsedInput
import com.aprekek.ai_advent.agentic_app.presentation.cli.SingleRequestExecutor
import java.io.BufferedReader

class ModelMetricsComparisonController(
    private val stdinReader: BufferedReader,
    private val config: AppConfig,
    private val mode: ChatMode,
    private val commandParser: CommandParser,
    private val consoleView: ConsoleView,
    private val loadingIndicator: LoadingIndicator,
    private val requestExecutor: SingleRequestExecutor,
    private val compareModelsWithMetricsUseCase: CompareModelsWithMetricsUseCase
) {
    suspend fun run(): Boolean {
        if (config.huggingFaceApiKey.isBlank()) {
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
                    consoleView.printHelp(config.model, config.responseLanguage, mode)
                    continue
                }

                is ParsedInput.Message -> {
                    val stages = listOf(
                        ModelVariant(
                            title = "Стадия 1: deepseek v3.2-reasoner",
                            provider = ProviderType.DeepSeek,
                            modelId = "deepseek-reasoner",
                            pricingMode = PricingMode.DeepSeekReasonerPricing
                        ),
                        ModelVariant(
                            title = "Стадия 2: deepseek v3.0",
                            provider = ProviderType.HuggingFace,
                            modelId = config.huggingFaceModelV30,
                            pricingMode = PricingMode.NotAvailable
                        ),
                        ModelVariant(
                            title = "Стадия 3: meta-llama/Llama-3.1-8B-Instruct",
                            provider = ProviderType.HuggingFace,
                            modelId = "meta-llama/Llama-3.1-8B-Instruct",
                            pricingMode = PricingMode.NotAvailable
                        )
                    )

                    val stageResult = loadingIndicator.withLoadingIndicator {
                        compareModelsWithMetricsUseCase(parsedInput.text, stages)
                    }

                    val stageOutputs = stageResult.getOrElse { error ->
                        consoleView.printMessageBlock(ErrorPrefix, error.message ?: "Unknown error")
                        return false
                    }

                    stageOutputs.forEach { output ->
                        println()
                        consoleView.printSectionSeparator()
                        println(output.stageTitle)
                        consoleView.printMessageBlock(AssistantPrefix, output.response)
                        consoleView.printModelMetrics(output.metrics, output.pricingMode)
                    }

                    println()
                    consoleView.printSectionSeparator()
                    println("Сравнение: качество, скорость, ресурсоёмкость")
                    val analysisPrompt = buildDifferentModelsAnalysisPrompt(parsedInput.text, stageOutputs)
                    val analysisResult = loadingIndicator.withLoadingIndicator {
                        requestExecutor.execute(analysisPrompt)
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

    private fun buildDifferentModelsAnalysisPrompt(
        userPrompt: String,
        stageOutputs: List<ModelStageResult>
    ): String {
        val outputsBlock = stageOutputs.joinToString(separator = "\n\n") { output ->
            val metrics = output.metrics
            """
            ${output.stageTitle}
            Метрики: время=${metrics?.responseTimeMs ?: "n/a"}ms, prompt_tokens=${metrics?.promptTokens ?: "n/a"}, completion_tokens=${metrics?.completionTokens ?: "n/a"}, total_tokens=${metrics?.totalTokens ?: "n/a"}
            Ответ:
            ${output.response}
            """.trimIndent()
        }

        return """
            Пользовательский промпт:
            $userPrompt
            
            Ниже ответы и метрики трёх моделей:
            $outputsBlock
            
            Сделай краткое сравнение по критериям:
            1) качество ответа
            2) скорость
            3) ресурсоёмкость (по токенам)
            
            Верни компактный вывод: лучший по качеству, лучший по скорости, самый экономный по токенам, и общий победитель.
        """.trimIndent()
    }
}
