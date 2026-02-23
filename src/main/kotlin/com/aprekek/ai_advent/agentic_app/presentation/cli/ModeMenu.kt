package com.aprekek.ai_advent.agentic_app.presentation.cli

import java.io.BufferedReader

class ModeMenu {
    fun selectMode(stdinReader: BufferedReader): ChatMode? {
        while (true) {
            println("Select mode:")
            println("1 - Standart mode")
            println("2 - Short mode (max tokens: 512, one paragraph, stop on empty line)")
            println("3 - Comparison mode")
            println("4 - Temperature diff mode")
            println("5 - Different models metrics compare")
            println("q - Exit")
            print("> ")
            System.out.flush()

            when (val option = stdinReader.readLine()?.trim()?.lowercase()) {
                "1" -> return ChatMode.Standard
                "2" -> return ChatMode.Short
                "3" -> return ChatMode.Comparison
                "4" -> return ChatMode.TemperatureDiff
                "5" -> return ChatMode.DifferentModelsMetricsCompare
                "q" -> return null
                else -> println("Unknown option $option. Please choose 1, 2, 3, 4, 5 or q.")
            }
        }
    }
}
