package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.ChatMessage
import com.aprekek.ai_advent.agentic_app.domain.model.GenerationOptions

interface ChatGateway {
    suspend fun generate(messages: List<ChatMessage>, options: GenerationOptions): String
}
