package com.aprekek.ai_advent.agentic_app.util

import com.aprekek.ai_advent.agentic_app.domain.port.IdGenerator
import java.util.*

class DefaultIdGenerator : IdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}
