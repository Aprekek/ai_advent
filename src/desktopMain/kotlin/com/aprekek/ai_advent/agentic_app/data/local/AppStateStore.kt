package com.aprekek.ai_advent.agentic_app.data.local

import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class AppStateStore(
    private val directories: AppDirectories,
    private val json: Json
) {
    private val mutex = Mutex()

    suspend fun read(): AppStateFile = mutex.withLock {
        readUnsafe()
    }

    suspend fun update(transform: (AppStateFile) -> AppStateFile): AppStateFile = mutex.withLock {
        val updated = transform(readUnsafe())
        writeUnsafe(updated)
        updated
    }

    private suspend fun readUnsafe(): AppStateFile = withContext(Dispatchers.IO) {
        val file = directories.appStateFile()
        if (!Files.exists(file)) {
            val initial = AppStateFile()
            Files.writeString(file, json.encodeToString(AppStateFile.serializer(), initial))
            return@withContext initial
        }

        val text = Files.readString(file)
        if (text.isBlank()) {
            return@withContext AppStateFile()
        }
        json.decodeFromString(AppStateFile.serializer(), text)
    }

    private suspend fun writeUnsafe(state: AppStateFile) = withContext(Dispatchers.IO) {
        Files.writeString(
            directories.appStateFile(),
            json.encodeToString(AppStateFile.serializer(), state)
        )
    }
}
