package com.aprekek.ai_advent.agentic_app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PanelLayoutState(
    val leftPanelVisible: Boolean = true,
    val rightPanelVisible: Boolean = true,
    val leftPanelWidthPx: Float = 280f,
    val rightPanelWidthPx: Float = 300f
)
