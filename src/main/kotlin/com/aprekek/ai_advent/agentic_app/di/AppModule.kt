package com.aprekek.ai_advent.agentic_app.di

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.app.EnvironmentProvider
import com.aprekek.ai_advent.agentic_app.app.SystemEnvironmentProvider
import com.aprekek.ai_advent.agentic_app.data.deepseek.ApiClientMetricsProvider
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekGateway
import com.aprekek.ai_advent.agentic_app.data.http.HttpClientFactory
import com.aprekek.ai_advent.agentic_app.data.provider.UnifiedModelExecutionGateway
import com.aprekek.ai_advent.agentic_app.data.provider.huggingface.HuggingFaceGateway
import com.aprekek.ai_advent.agentic_app.data.state.InMemoryConversationState
import com.aprekek.ai_advent.agentic_app.domain.usecase.CompareModelsWithMetricsUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.CliRunner
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ModeMenu
import com.aprekek.ai_advent.agentic_app.presentation.cli.SingleRequestExecutor
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ComparisonController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ModelMetricsComparisonController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.StandardChatController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.TemperatureDiffController
import io.ktor.client.HttpClient
import java.io.BufferedReader
import java.io.Closeable

class AppModule private constructor(
    val cliRunner: CliRunner,
    private val httpClient: HttpClient
) : Closeable {
    override fun close() {
        httpClient.close()
    }

    companion object {
        fun fromEnvironment(
            stdinReader: BufferedReader,
            environmentProvider: EnvironmentProvider = SystemEnvironmentProvider
        ): AppModule {
            val config = AppConfig.fromEnvironment(environmentProvider)
            val httpClient = HttpClientFactory.createDefault()

            val apiClient = DeepSeekApiClient(httpClient)
            val deepSeekGateway = DeepSeekGateway(apiClient, config)
            val huggingFaceGateway = HuggingFaceGateway(apiClient, config)
            val modelExecutionGateway = UnifiedModelExecutionGateway(deepSeekGateway, huggingFaceGateway)
            val metricsProvider = ApiClientMetricsProvider(apiClient)
            val compareModelsWithMetricsUseCase = CompareModelsWithMetricsUseCase(
                modelExecutionGateway = modelExecutionGateway,
                metricsProvider = metricsProvider
            )
            val commandParser = CommandParser()
            val consoleView = ConsoleView()
            val loadingIndicator = LoadingIndicator()
            val modeMenu = ModeMenu()
            val requestExecutor = SingleRequestExecutor(deepSeekGateway)
            val conversationState = InMemoryConversationState()

            val cliRunner = CliRunner(
                stdinReader = stdinReader,
                consoleView = consoleView,
                modeMenu = modeMenu,
                standardControllerFactory = { mode ->
                    StandardChatController(
                        stdinReader = stdinReader,
                        config = config,
                        mode = mode,
                        sendMessageUseCase = SendMessageUseCase(
                            chatGateway = deepSeekGateway,
                            conversationState = conversationState
                        ),
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator
                    )
                },
                comparisonControllerFactory = { mode ->
                    ComparisonController(
                        stdinReader = stdinReader,
                        config = config,
                        mode = mode,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator,
                        requestExecutor = requestExecutor
                    )
                },
                temperatureDiffControllerFactory = { mode ->
                    TemperatureDiffController(
                        stdinReader = stdinReader,
                        config = config,
                        mode = mode,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator,
                        requestExecutor = requestExecutor
                    )
                },
                modelMetricsComparisonControllerFactory = { mode ->
                    ModelMetricsComparisonController(
                        stdinReader = stdinReader,
                        config = config,
                        mode = mode,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator,
                        requestExecutor = requestExecutor,
                        compareModelsWithMetricsUseCase = compareModelsWithMetricsUseCase
                    )
                }
            )

            return AppModule(
                cliRunner = cliRunner,
                httpClient = httpClient
            )
        }
    }
}
