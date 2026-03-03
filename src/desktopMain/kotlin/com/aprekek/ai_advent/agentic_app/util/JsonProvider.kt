package com.aprekek.ai_advent.agentic_app.util

import kotlinx.serialization.json.Json

fun defaultJson(): Json = Json {
    ignoreUnknownKeys = true
    prettyPrint = true
    encodeDefaults = true
}
