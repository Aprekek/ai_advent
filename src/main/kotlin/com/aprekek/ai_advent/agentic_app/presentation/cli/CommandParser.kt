package com.aprekek.ai_advent.agentic_app.presentation.cli

sealed interface ParsedInput {
    data object Blank : ParsedInput
    data object Exit : ParsedInput
    data object Back : ParsedInput
    data object Help : ParsedInput
    data class Message(val text: String) : ParsedInput
}

class CommandParser {
    fun parse(rawInput: String?): ParsedInput {
        val input = rawInput?.trim() ?: return ParsedInput.Exit
        if (input.isBlank()) {
            return ParsedInput.Blank
        }
        if (input.equals("/exit", ignoreCase = true)) {
            return ParsedInput.Exit
        }
        if (input.equals("q", ignoreCase = true)) {
            return ParsedInput.Back
        }
        if (input == "/help") {
            return ParsedInput.Help
        }
        return ParsedInput.Message(input)
    }
}
