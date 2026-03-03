package com.aprekek.ai_advent.agentic_app.data.security

import com.aprekek.ai_advent.agentic_app.data.local.AppDirectories
import com.aprekek.ai_advent.agentic_app.domain.port.ApiKeyRepository
import com.aprekek.ai_advent.agentic_app.util.OsFamily
import com.aprekek.ai_advent.agentic_app.util.detectOsFamily
import java.nio.charset.StandardCharsets
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SecureApiKeyRepository(
    private val directories: AppDirectories,
    private val osFamily: OsFamily = detectOsFamily()
) : ApiKeyRepository {
    override suspend fun saveApiKey(profileId: String, apiKey: String) {
        when (osFamily) {
            OsFamily.MAC -> saveToMacKeychain(profileId, apiKey)
            OsFamily.WINDOWS -> saveWithWindowsDpapi(profileId, apiKey)
            OsFamily.OTHER -> throw IllegalStateException(
                "Текущая ОС не поддерживается для безопасного хранения API key"
            )
        }
    }

    override suspend fun getApiKey(profileId: String): String? {
        return when (osFamily) {
            OsFamily.MAC -> readFromMacKeychain(profileId)
            OsFamily.WINDOWS -> readWithWindowsDpapi(profileId)
            OsFamily.OTHER -> null
        }
    }

    private suspend fun saveToMacKeychain(profileId: String, apiKey: String) = withContext(Dispatchers.IO) {
        val result = runCommand(
            listOf(
                "/usr/bin/security",
                "add-generic-password",
                "-a",
                account(profileId),
                "-s",
                serviceName(),
                "-w",
                apiKey,
                "-U"
            )
        )

        if (result.exitCode != 0) {
            throw IllegalStateException("Не удалось сохранить API key в Keychain: ${result.output}")
        }
    }

    private suspend fun readFromMacKeychain(profileId: String): String? = withContext(Dispatchers.IO) {
        val result = runCommand(
            listOf(
                "/usr/bin/security",
                "find-generic-password",
                "-a",
                account(profileId),
                "-s",
                serviceName(),
                "-w"
            )
        )

        if (result.exitCode != 0) {
            return@withContext null
        }

        result.output.trim().ifEmpty { null }
    }

    private suspend fun saveWithWindowsDpapi(profileId: String, apiKey: String) = withContext(Dispatchers.IO) {
        val path = directories.windowsProtectedKeyFile(profileId).toAbsolutePath().toString()
        val keyBase64 = Base64.getEncoder().encodeToString(apiKey.toByteArray(StandardCharsets.UTF_8))
        val script = "\$bytes=[Convert]::FromBase64String('${psLiteral(keyBase64)}');" +
            "\$enc=[System.Security.Cryptography.ProtectedData]::Protect(\$bytes,\$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser);" +
            "[IO.File]::WriteAllBytes('${psLiteral(path)}',\$enc)"

        val result = runCommand(
            listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
            )
        )

        if (result.exitCode != 0) {
            throw IllegalStateException("Не удалось сохранить API key через DPAPI: ${result.output}")
        }
    }

    private suspend fun readWithWindowsDpapi(profileId: String): String? = withContext(Dispatchers.IO) {
        val path = directories.windowsProtectedKeyFile(profileId).toAbsolutePath().toString()
        val script = "if (-not (Test-Path '${psLiteral(path)}')) { exit 2 };" +
            "\$enc=[IO.File]::ReadAllBytes('${psLiteral(path)}');" +
            "\$dec=[System.Security.Cryptography.ProtectedData]::Unprotect(\$enc,\$null,[System.Security.Cryptography.DataProtectionScope]::CurrentUser);" +
            "[Convert]::ToBase64String(\$dec)"

        val result = runCommand(
            listOf(
                "powershell",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                script
            )
        )

        if (result.exitCode == 2) {
            return@withContext null
        }

        if (result.exitCode != 0) {
            throw IllegalStateException("Не удалось прочитать API key через DPAPI: ${result.output}")
        }

        val decoded = runCatching {
            String(Base64.getDecoder().decode(result.output.trim()), StandardCharsets.UTF_8)
        }.getOrNull()

        decoded?.ifEmpty { null }
    }

    private fun runCommand(command: List<String>): CommandResult {
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        return CommandResult(exitCode = exit, output = output)
    }

    private fun account(profileId: String): String = "apragent-$profileId"

    private fun serviceName(): String = "AprAgent.DeepSeek"

    private fun psLiteral(value: String): String = value.replace("'", "''")

    private data class CommandResult(
        val exitCode: Int,
        val output: String
    )
}
