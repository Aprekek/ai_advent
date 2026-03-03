package com.aprekek.ai_advent.agentic_app.domain.port

import com.aprekek.ai_advent.agentic_app.domain.model.ChatRequest
import com.aprekek.ai_advent.agentic_app.domain.model.StreamEvent
import kotlinx.coroutines.flow.Flow

interface ChatStreamingGateway {
    fun streamChat(request: ChatRequest): Flow<StreamEvent>
}
