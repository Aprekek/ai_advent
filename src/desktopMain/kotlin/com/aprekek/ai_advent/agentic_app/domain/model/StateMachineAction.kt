package com.aprekek.ai_advent.agentic_app.domain.model

sealed interface StateMachineAction {
    data class UserInput(val value: String) : StateMachineAction
    data object ApprovePlan : StateMachineAction
    data object SkipClarificationToExecution : StateMachineAction
    data object Continue : StateMachineAction
    data object ValidationRework : StateMachineAction
    data object ValidationAcceptCurrent : StateMachineAction
    data object Stop : StateMachineAction
}
