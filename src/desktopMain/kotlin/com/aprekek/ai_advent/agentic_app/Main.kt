package com.aprekek.ai_advent.agentic_app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.aprekek.ai_advent.agentic_app.di.initKoin
import com.aprekek.ai_advent.agentic_app.presentation.state.AppViewModel
import com.aprekek.ai_advent.agentic_app.presentation.ui.AprAgentApp
import io.ktor.client.HttpClient

fun main() = application {
    val koinApp = remember { initKoin() }
    val viewModel = remember { koinApp.koin.get<AppViewModel>() }
    val windowState = remember { WindowState(placement = WindowPlacement.Maximized) }

    Window(
        state = windowState,
        onCloseRequest = {
            viewModel.dispose()
            runCatching { koinApp.koin.get<HttpClient>().close() }
            koinApp.close()
            exitApplication()
        },
        title = "AprAgent"
    ) {
        AprAgentApp(viewModel)
    }
}
