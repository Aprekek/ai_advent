package com.aprekek.ai_advent.agentic_app.presentation.cli.mode

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.data.deepseek.CallMetrics
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.ProviderRequestContext
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions
import com.aprekek.ai_advent.agentic_app.presentation.cli.AssistantPrefix
import com.aprekek.ai_advent.agentic_app.presentation.cli.ChatMode
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.CostMode
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
    private val apiClient: DeepSeekApiClient
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
                        ModelStage(
                            title = "Стадия 1: deepseek v3.2-reasoner",
                            options = GenerationOptions.Standard,
                            requestContext = ProviderRequestContext(
                                model = "deepseek-reasoner",
                                apiKey = config.apiKey,
                                baseUrl = config.baseUrl
                            ),
                            costMode = CostMode.DeepSeekReasonerPricing
                        ),
                        ModelStage(
                            title = "Стадия 2: deepseek v3.0",
                            options = GenerationOptions.Standard,
                            requestContext = ProviderRequestContext(
                                model = config.huggingFaceModelV30,
                                apiKey = config.huggingFaceApiKey,
                                baseUrl = config.huggingFaceBaseUrl
                            ),
                            costMode = CostMode.NotAvailable
                        ),
                        ModelStage(
                            title = "Стадия 3: meta-llama/Llama-3.1-8B-Instruct",
                            options = GenerationOptions.Standard,
                            requestContext = ProviderRequestContext(
                                model = "meta-llama/Llama-3.1-8B-Instruct",
                                apiKey = config.huggingFaceApiKey,
                                baseUrl = config.huggingFaceBaseUrl
                            ),
                            costMode = CostMode.NotAvailable
                        )
                    )

                    val stageOutputs = mutableListOf<StageOutput>()
                    for (stage in stages) {
                        println()
                        consoleView.printSectionSeparator()
                        println(stage.title)
                        val result = loadingIndicator.withLoadingIndicator {
                            requestExecutor.execute(
                                prompt = parsedInput.text,
                                options = stage.options,
                                requestContext = stage.requestContext
                            )
                        }

                        result.onSuccess { response ->
                            val metricsSnapshot = apiClient.lastCallMetrics
                            stageOutputs += StageOutput(
                                stageTitle = stage.title,
                                response = response,
                                metrics = metricsSnapshot
                            )
                            consoleView.printMessageBlock(AssistantPrefix, response)
                            consoleView.printModelMetrics(metricsSnapshot, stage.costMode)
                        }.onFailure { exception ->
                            consoleView.printMessageBlock(ErrorPrefix, exception.message ?: "Unknown error")
                            return false
                        }
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
        stageOutputs: List<StageOutput>
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

private data class ModelStage(
    val title: String,
    val options: GenerationOptions,
    val requestContext: ProviderRequestContext,
    val costMode: CostMode
)

private data class StageOutput(
    val stageTitle: String,
    val response: String,
    val metrics: CallMetrics?
)
