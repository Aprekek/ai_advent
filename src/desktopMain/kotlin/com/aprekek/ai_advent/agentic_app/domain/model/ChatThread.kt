package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatThread(
    val id: String,
    val userId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val contextItems: List<ChatContextItem> = emptyList(),
    val mode: ChatMode = ChatMode.STANDARD,
    val stateMachineSession: StateMachineSession? = null
)

@Serializable
data class ChatContextItem(
    val id: String,
    val value: String,
    val createdAt: Long
)
