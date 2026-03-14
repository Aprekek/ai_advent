package com.aprekek.ai_advent.agentic_app.domain.model

sealed interface StateMachineProgress {
    data class PartialAssistant(val content: String) : StateMachineProgress
    data class SessionUpdated(val session: StateMachineSession) : StateMachineProgress
    data class Reconnecting(val attempt: Int) : StateMachineProgress
    data object Completed : StateMachineProgress
}
