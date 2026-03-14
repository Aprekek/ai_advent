package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StateMachineSession(
    val sessionId: String,
    val stage: StateMachineStage = StateMachineStage.PLANNING,
    val doneStatus: StateMachineDoneStatus? = null,
    val waitingForUserInput: Boolean = true,
    val task: String = "",
    val sessionStartedAt: Long = 0L,
    val planDraft: String = "",
    val approvedPlan: String = "",
    val executionResult: String = "",
    val validationResult: String = "",
    val lastFailureReason: String? = null
)

@Serializable
enum class StateMachineStage {
    PLANNING,
    CLARIFICATION,
    EXECUTION,
    VALIDATION,
    DONE
}

@Serializable
enum class StateMachineDoneStatus {
    DONE,
    CANCELED,
    FAILED
}
