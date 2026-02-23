package com.aprekek.ai_advent.agentic_app

import com.aprekek.ai_advent.agentic_app.di.AppModule
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val stdinReader = BufferedReader(InputStreamReader(System.`in`))

    val appModule = runCatching {
        AppModule.fromEnvironment(stdinReader = stdinReader)
    }.getOrElse { error ->
        println("Configuration error: ${error.message}")
        return@runBlocking
    }

    appModule.use { module ->
        module.cliRunner.run()
    }
}
