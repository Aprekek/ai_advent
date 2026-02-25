package com.aprekek.ai_advent.agentic_app.di

import com.aprekek.ai_advent.agentic_app.data.config.EnvConfigProvider
import com.aprekek.ai_advent.agentic_app.data.config.EnvironmentProvider
import com.aprekek.ai_advent.agentic_app.data.config.SystemEnvironmentProvider
import com.aprekek.ai_advent.agentic_app.data.http.HttpClientFactory
import com.aprekek.ai_advent.agentic_app.data.provider.UnifiedModelExecutionGateway
import com.aprekek.ai_advent.agentic_app.data.provider.deepseek.ApiClientMetricsProvider
import com.aprekek.ai_advent.agentic_app.data.provider.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.provider.deepseek.DeepSeekGateway
import com.aprekek.ai_advent.agentic_app.data.provider.huggingface.HuggingFaceApiClient
import com.aprekek.ai_advent.agentic_app.data.provider.huggingface.HuggingFaceGateway
import com.aprekek.ai_advent.agentic_app.data.state.JvmSqlDriverFactory
import com.aprekek.ai_advent.agentic_app.data.state.SqlDelightConversationState
import com.aprekek.ai_advent.agentic_app.domain.usecase.BuildModelComparisonPromptUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.BuildTemperatureComparisonPromptUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.ClearConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CompareModelsWithMetricsUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.ComparePromptStrategiesUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.CompareTemperatureUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GenerateSingleResponseUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.GetConversationHistoryUseCase
import com.aprekek.ai_advent.agentic_app.domain.usecase.SendMessageUseCase
import com.aprekek.ai_advent.agentic_app.presentation.cli.CliRunner
import com.aprekek.ai_advent.agentic_app.presentation.cli.CommandParser
import com.aprekek.ai_advent.agentic_app.presentation.cli.ConsoleView
import com.aprekek.ai_advent.agentic_app.presentation.cli.LoadingIndicator
import com.aprekek.ai_advent.agentic_app.presentation.cli.ModeMenu
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ComparisonController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ModelMetricsComparisonController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.StandardChatController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.TemperatureDiffController
import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import java.io.BufferedReader
import java.io.Closeable

class AppModule private constructor(
    val cliRunner: CliRunner,
    private val httpClient: HttpClient,
    private val sqlDriver: SqlDriver
) : Closeable {
    override fun close() {
        sqlDriver.close()
        httpClient.close()
    }

    companion object {
        fun fromEnvironment(
            stdinReader: BufferedReader,
            environmentProvider: EnvironmentProvider = SystemEnvironmentProvider
        ): AppModule {
            val envConfigProvider = EnvConfigProvider(environmentProvider)
            val config = envConfigProvider.appConfig
            val httpClient = HttpClientFactory.createDefault()

            val deepSeekApiClient = DeepSeekApiClient(httpClient)
            val deepSeekGateway = DeepSeekGateway(deepSeekApiClient, config)
            val huggingFaceApiClient = HuggingFaceApiClient(httpClient)
            val huggingFaceGateway = HuggingFaceGateway(huggingFaceApiClient, config)
            val sqlDriver = JvmSqlDriverFactory(dbPath = defaultSessionDbPath()).createDriver()
            val conversationState = SqlDelightConversationState(sqlDriver)

            val modelExecutionGateway = UnifiedModelExecutionGateway(deepSeekGateway, huggingFaceGateway)
            val metricsProvider = ApiClientMetricsProvider(deepSeekApiClient, huggingFaceApiClient)

            val sendMessageUseCase = SendMessageUseCase(
                chatGateway = deepSeekGateway,
                conversationState = conversationState
            )
            val getConversationHistoryUseCase = GetConversationHistoryUseCase(conversationState)
            val clearConversationHistoryUseCase = ClearConversationHistoryUseCase(conversationState)
            val comparePromptStrategiesUseCase = ComparePromptStrategiesUseCase(deepSeekGateway)
            val compareTemperatureUseCase = CompareTemperatureUseCase(deepSeekGateway)
            val generateSingleResponseUseCase = GenerateSingleResponseUseCase(deepSeekGateway)
            val compareModelsWithMetricsUseCase = CompareModelsWithMetricsUseCase(
                modelExecutionGateway = modelExecutionGateway,
                metricsProvider = metricsProvider
            )
            val buildTemperatureComparisonPromptUseCase = BuildTemperatureComparisonPromptUseCase()
            val buildModelComparisonPromptUseCase = BuildModelComparisonPromptUseCase()

            val commandParser = CommandParser()
            val consoleView = ConsoleView()
            val loadingIndicator = LoadingIndicator()
            val modeMenu = ModeMenu()

            val cliRunner = CliRunner(
                stdinReader = stdinReader,
                consoleView = consoleView,
                modeMenu = modeMenu,
                standardControllerFactory = { mode ->
                    StandardChatController(
                        stdinReader = stdinReader,
                        configProvider = envConfigProvider,
                        mode = mode,
                        sendMessageUseCase = sendMessageUseCase,
                        getConversationHistoryUseCase = getConversationHistoryUseCase,
                        clearConversationHistoryUseCase = clearConversationHistoryUseCase,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator
                    )
                },
                comparisonControllerFactory = { mode ->
                    ComparisonController(
                        stdinReader = stdinReader,
                        configProvider = envConfigProvider,
                        mode = mode,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator,
                        comparePromptStrategiesUseCase = comparePromptStrategiesUseCase
                    )
                },
                temperatureDiffControllerFactory = { mode ->
                    TemperatureDiffController(
                        stdinReader = stdinReader,
                        configProvider = envConfigProvider,
                        mode = mode,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator,
                        compareTemperatureUseCase = compareTemperatureUseCase,
                        buildTemperatureComparisonPromptUseCase = buildTemperatureComparisonPromptUseCase,
                        generateSingleResponseUseCase = generateSingleResponseUseCase
                    )
                },
                modelMetricsComparisonControllerFactory = { mode ->
                    ModelMetricsComparisonController(
                        stdinReader = stdinReader,
                        configProvider = envConfigProvider,
                        mode = mode,
                        commandParser = commandParser,
                        consoleView = consoleView,
                        loadingIndicator = loadingIndicator,
                        compareModelsWithMetricsUseCase = compareModelsWithMetricsUseCase,
                        buildModelComparisonPromptUseCase = buildModelComparisonPromptUseCase,
                        generateSingleResponseUseCase = generateSingleResponseUseCase
                    )
                }
            )

            return AppModule(cliRunner = cliRunner, httpClient = httpClient, sqlDriver = sqlDriver)
        }

        private fun defaultSessionDbPath(): String {
            val homeDir = System.getProperty("user.home").orEmpty().ifBlank { "." }
            return "$homeDir/.apragent/session_history.db"
        }
    }
}
