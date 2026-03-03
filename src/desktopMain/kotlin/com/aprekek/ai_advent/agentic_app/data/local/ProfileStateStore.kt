package com.aprekek.ai_advent.agentic_app.data.local

import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class ProfileStateStore(
    private val directories: AppDirectories,
    private val json: Json
) {
    private val mutex = Mutex()

    suspend fun read(profileId: String): ProfileDataFile = mutex.withLock {
        readUnsafe(profileId)
    }

    suspend fun update(
        profileId: String,
        transform: (ProfileDataFile) -> ProfileDataFile
    ): ProfileDataFile = mutex.withLock {
        val updated = transform(readUnsafe(profileId))
        writeUnsafe(profileId, updated)
        updated
    }

    private suspend fun readUnsafe(profileId: String): ProfileDataFile = withContext(Dispatchers.IO) {
        val file = directories.profileDataFile(profileId)
        if (!Files.exists(file)) {
            val initial = ProfileDataFile()
            Files.writeString(file, json.encodeToString(ProfileDataFile.serializer(), initial))
            return@withContext initial
        }

        val text = Files.readString(file)
        if (text.isBlank()) {
            return@withContext ProfileDataFile()
        }

        json.decodeFromString(ProfileDataFile.serializer(), text)
    }

    private suspend fun writeUnsafe(profileId: String, data: ProfileDataFile) = withContext(Dispatchers.IO) {
        Files.writeString(
            directories.profileDataFile(profileId),
            json.encodeToString(ProfileDataFile.serializer(), data)
        )
    }
}
