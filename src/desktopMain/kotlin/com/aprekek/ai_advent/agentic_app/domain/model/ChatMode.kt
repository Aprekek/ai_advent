package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ChatMode {
    STANDARD,
    STATE_MACHINE
}
