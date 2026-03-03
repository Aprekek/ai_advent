package com.aprekek.ai_advent.agentic_app.domain.model

sealed interface SendMessageProgress {
    data class PartialAssistant(val content: String) : SendMessageProgress
    data class Reconnecting(val attempt: Int) : SendMessageProgress
    data object Completed : SendMessageProgress
}
