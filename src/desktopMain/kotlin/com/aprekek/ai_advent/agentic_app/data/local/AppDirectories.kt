package com.aprekek.ai_advent.agentic_app.data.local

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.deleteIfExists

class AppDirectories(
    private val baseDir: Path = defaultBaseDir()
) {
    init {
        Files.createDirectories(baseDir)
    }

    fun appStateFile(): Path = baseDir.resolve("app_state.json")

    fun profileDirectory(profileId: String): Path {
        val dir = baseDir.resolve("profiles").resolve(profileId)
        Files.createDirectories(dir)
        return dir
    }

    fun profileDataFile(profileId: String): Path = profileDirectory(profileId).resolve("profile_data.json")

    fun windowsGlobalProtectedKeyFile(): Path = baseDir.resolve("deepseek_api_key.bin")

    fun windowsProtectedKeyFile(profileId: String): Path = profileDirectory(profileId).resolve("deepseek_api_key.bin")

    fun deleteProfileDirectory(profileId: String) {
        val dir = baseDir.resolve("profiles").resolve(profileId)
        if (!Files.exists(dir)) return

        Files.walk(dir).use { stream ->
            stream
                .sorted(Comparator.reverseOrder())
                .forEach { path -> path.deleteIfExists() }
        }
    }

    companion object {
        private fun defaultBaseDir(): Path {
            val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
            return Paths.get(home).resolve(".apragent-desktop")
        }
    }
}
