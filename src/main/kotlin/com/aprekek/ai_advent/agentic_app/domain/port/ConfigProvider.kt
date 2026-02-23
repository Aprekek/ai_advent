package com.aprekek.ai_advent.agentic_app.domain.port

interface ConfigProvider {
    fun model(): String
    fun responseLanguage(): String
    fun huggingFaceModelV30(): String
    fun hasHuggingFaceApiKey(): Boolean
}
