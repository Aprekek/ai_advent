package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatThread(
    val id: String,
    val userId: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long
)
