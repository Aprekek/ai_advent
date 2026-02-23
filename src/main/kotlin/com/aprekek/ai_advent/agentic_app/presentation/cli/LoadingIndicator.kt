package com.aprekek.ai_advent.agentic_app.presentation.cli

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoadingIndicator {
    suspend fun <T> withLoadingIndicator(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        val frames = listOf("|", "/", "-", "\\")
        val spinnerJob = launch {
            var index = 0
            while (isActive) {
                print("\rassistant> thinking ${frames[index % frames.size]}")
                System.out.flush()
                index++
                delay(120)
            }
        }

        try {
            block()
        } finally {
            spinnerJob.cancel()
            print("\r                              \r")
            System.out.flush()
        }
    }
}
