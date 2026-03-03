package com.aprekek.ai_advent.agentic_app.util

enum class OsFamily {
    MAC,
    WINDOWS,
    OTHER
}

fun detectOsFamily(osName: String = System.getProperty("os.name").orEmpty()): OsFamily {
    val normalized = osName.lowercase()
    return when {
        normalized.contains("mac") -> OsFamily.MAC
        normalized.contains("win") -> OsFamily.WINDOWS
        else -> OsFamily.OTHER
    }
}
