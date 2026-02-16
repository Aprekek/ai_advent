package com.aprekek.ai_advent.agentic_app.cli

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TerminalInput {
    fun lines(prompt: String = "you> "): Flow<String> = flow {
        while (true) {
            print(prompt)
            System.out.flush()

            val line = readlnOrNull() ?: break
            emit(line)
        }
    }
}
