package com.aprekek.ai_advent.agentic_app.util

import com.aprekek.ai_advent.agentic_app.domain.port.TimeProvider

class SystemTimeProvider : TimeProvider {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
