package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val createdAt: Long
)
