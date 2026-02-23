package com.aprekek.ai_advent.agentic_app.presentation.cli

import com.aprekek.ai_advent.agentic_app.domain.model.Metrics
import com.aprekek.ai_advent.agentic_app.domain.model.PricingMode
import java.io.BufferedReader

class ConsoleView {
    fun clearTerminal() {
        print("\u001B[H\u001B[2J")
        System.out.flush()
    }

    fun printPrompt() {
        println()
        print(UserPrompt)
        System.out.flush()
    }

    fun printMessageBlock(prefix: String, text: String) {
        println()
        printWrappedResponse(prefix, text)
        println()
    }

    fun printHelp(model: String, responseLanguage: String, mode: ChatMode) {
        println("Current model: $model")
        println("Response language: $responseLanguage")
        println("Current mode: ${mode.displayName}")
        println("Use q to return to mode selection (history resets after mode switch).")
        println("Use /exit to stop the app.")
    }

    fun printSectionSeparator() {
        println(SectionSeparator)
    }

    fun printModelMetrics(metrics: Metrics?, costMode: PricingMode) {
        val responseTime = metrics?.responseTimeMs?.let { "${it} ms" } ?: "n/a"
        val promptTokens = metrics?.promptTokens?.toString() ?: "n/a"
        val completionTokens = metrics?.completionTokens?.toString() ?: "n/a"
        val totalTokens = metrics?.totalTokens?.toString() ?: "n/a"

        println("Метрики:")
        println("Время ответа: $responseTime")
        println("Токены: prompt=$promptTokens, completion=$completionTokens, total=$totalTokens")
        when (costMode) {
            PricingMode.DeepSeekReasonerPricing -> {
                val prompt = metrics?.promptTokens
                val completion = metrics?.completionTokens
                if (prompt == null || completion == null) {
                    println("Стоимость: n/a")
                } else {
                    val inputHit = (prompt / 1_000_000.0) * DeepSeekInputCostCacheHitPer1M
                    val inputMiss = (prompt / 1_000_000.0) * DeepSeekInputCostCacheMissPer1M
                    val outputCost = (completion / 1_000_000.0) * DeepSeekOutputCostPer1M
                    val totalHit = inputHit + outputCost
                    val totalMiss = inputMiss + outputCost
                    println("Стоимость (cache hit): $${"%.6f".format(totalHit)}")
                    println("Стоимость (cache miss): $${"%.6f".format(totalMiss)}")
                }
            }

            PricingMode.NotAvailable -> println("Стоимость: n/a")
        }
    }

    fun waitForEnterToContinue(stdinReader: BufferedReader): Boolean {
        println()
        println("Нажмите Enter для перехода к следующему варианту (или /exit для выхода):")
        print("> ")
        System.out.flush()

        val input = stdinReader.readLine() ?: return true
        return input.trim().equals("/exit", ignoreCase = true)
    }

    private fun printWrappedResponse(prefix: String, text: String) {
        val content = text.trim()
        if (content.isEmpty()) {
            if (prefix.isNotEmpty()) {
                println(prefix)
            }
            return
        }

        val availableForFirstLine = (MaxConsoleLineLength - prefix.length).coerceAtLeast(1)
        val availableForNextLines = MaxConsoleLineLength.coerceAtLeast(1)
        var prefixPrinted = prefix.isEmpty()

        val paragraphs = content.replace("\r\n", "\n").split('\n')
        paragraphs.forEach { paragraph ->
            if (paragraph.isBlank()) {
                println()
                return@forEach
            }

            var remaining = paragraph.trim()
            var firstChunkInParagraph = true
            while (remaining.isNotEmpty()) {
                val available = if (!prefixPrinted && firstChunkInParagraph) {
                    availableForFirstLine
                } else {
                    availableForNextLines
                }
                if (remaining.length <= available) {
                    val linePrefix = if (!prefixPrinted && firstChunkInParagraph) prefix else ""
                    println(linePrefix + remaining)
                    prefixPrinted = true
                    break
                }

                val splitAt = remaining
                    .lastIndexOf(' ', startIndex = available)
                    .takeIf { it > 0 }
                    ?: available

                val chunk = remaining.substring(0, splitAt).trimEnd()
                val linePrefix = if (!prefixPrinted && firstChunkInParagraph) prefix else ""
                println(linePrefix + chunk)
                remaining = remaining.substring(splitAt).trimStart()
                prefixPrinted = true
                firstChunkInParagraph = false
            }
        }
    }
}
