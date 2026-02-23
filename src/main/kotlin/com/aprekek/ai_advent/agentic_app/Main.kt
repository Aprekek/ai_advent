package com.aprekek.ai_advent.agentic_app

import com.aprekek.ai_advent.agentic_app.app.AppConfig
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekApiClient
import com.aprekek.ai_advent.agentic_app.data.deepseek.DeepSeekChatRepository
import com.aprekek.ai_advent.agentic_app.data.state.InMemoryConversationState
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
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main() = runBlocking {
    val stdinReader = BufferedReader(InputStreamReader(System.`in`))
    val config = runCatching { AppConfig.fromEnvironment() }.getOrElse { error ->
        println("Configuration error: ${error.message}")
        return@runBlocking
    }

    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                }
            )
        }
    }

    val apiClient = DeepSeekApiClient(httpClient, config)
    val chatRepository = DeepSeekChatRepository(apiClient)
    val commandParser = CommandParser()
    val consoleView = ConsoleView()
    val loadingIndicator = LoadingIndicator()
    val modeMenu = ModeMenu()
    val requestExecutor = SingleRequestExecutor(chatRepository)
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
                    chatGateway = chatRepository,
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
                apiClient = apiClient
            )
        }
    )

    try {
        cliRunner.run()
    } finally {
        httpClient.close()
    }
}
