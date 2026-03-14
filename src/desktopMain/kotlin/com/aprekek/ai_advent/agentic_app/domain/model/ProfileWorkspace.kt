package com.aprekek.ai_advent.agentic_app.domain.model

data class ProfileWorkspace(
    val chats: List<ChatThread>,
    val selectedChatId: String?,
    val selectedChatMode: ChatMode = ChatMode.STANDARD,
    val stateMachineSession: StateMachineSession? = null,
    val messages: List<ChatMessage>,
    val hasApiKey: Boolean
)
