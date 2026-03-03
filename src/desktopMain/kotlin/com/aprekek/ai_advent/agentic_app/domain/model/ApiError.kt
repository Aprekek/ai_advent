package com.aprekek.ai_advent.agentic_app.domain.model

sealed class ApiError(message: String) : RuntimeException(message) {
    class Unauthorized(message: String = "Неверный или отсутствующий API key") : ApiError(message)
    class RateLimited(message: String = "Превышен лимит запросов. Повторите позже") : ApiError(message)
    class Server(message: String = "Проблема на стороне DeepSeek") : ApiError(message)
    class Network(message: String = "Сетевая ошибка") : ApiError(message)
    class Timeout(message: String = "Превышено время ожидания ответа") : ApiError(message)
    class Unknown(message: String) : ApiError(message)
}
