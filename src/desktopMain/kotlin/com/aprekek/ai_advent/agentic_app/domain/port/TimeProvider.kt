package com.aprekek.ai_advent.agentic_app.domain.port

interface TimeProvider {
    fun nowMillis(): Long
}
