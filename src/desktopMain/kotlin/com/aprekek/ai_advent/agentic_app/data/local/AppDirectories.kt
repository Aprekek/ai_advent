package com.aprekek.ai_advent.agentic_app.data.local

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

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

    fun windowsProtectedKeyFile(profileId: String): Path = profileDirectory(profileId).resolve("deepseek_api_key.bin")

    companion object {
        private fun defaultBaseDir(): Path {
            val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
            return Paths.get(home).resolve(".apragent-desktop")
        }
    }
}
