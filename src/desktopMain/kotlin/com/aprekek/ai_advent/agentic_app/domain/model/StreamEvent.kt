package com.aprekek.ai_advent.agentic_app.domain.model

sealed interface StreamEvent {
    data object Started : StreamEvent
    data class Delta(val value: String) : StreamEvent
    data class Reconnecting(val attempt: Int) : StreamEvent
    data object Completed : StreamEvent
}
