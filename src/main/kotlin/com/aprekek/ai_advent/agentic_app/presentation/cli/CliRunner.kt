package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ComparisonController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.ModelMetricsComparisonController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.StandardChatController
import com.aprekek.ai_advent.agentic_app.presentation.cli.mode.TemperatureDiffController
import java.io.BufferedReader

class CliRunner(
    private val stdinReader: BufferedReader,
    private val consoleView: ConsoleView,
    private val modeMenu: ModeMenu,
    private val standardControllerFactory: (ChatMode) -> StandardChatController,
    private val comparisonControllerFactory: (ChatMode) -> ComparisonController,
    private val temperatureDiffControllerFactory: (ChatMode) -> TemperatureDiffController,
    private val modelMetricsComparisonControllerFactory: (ChatMode) -> ModelMetricsComparisonController
) {
    suspend fun run() {
        consoleView.clearTerminal()
        println("DeepSeek CLI started.")

        while (true) {
            val mode = modeMenu.selectMode(stdinReader) ?: break
            val shouldExit = when (mode) {
                ChatMode.Comparison -> comparisonControllerFactory(mode).run()
                ChatMode.TemperatureDiff -> temperatureDiffControllerFactory(mode).run()
                ChatMode.DifferentModelsMetricsCompare -> modelMetricsComparisonControllerFactory(mode).run()
                ChatMode.Standard, ChatMode.Short -> standardControllerFactory(mode).run()
            }
            if (shouldExit) {
                return
            }
            println()
        }
    }
}
