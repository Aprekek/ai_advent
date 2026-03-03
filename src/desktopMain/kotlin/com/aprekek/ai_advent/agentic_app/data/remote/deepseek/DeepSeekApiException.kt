package com.aprekek.ai_advent.agentic_app.data.remote.deepseek

class DeepSeekApiException(
    val statusCode: Int,
    message: String
) : RuntimeException(message)
