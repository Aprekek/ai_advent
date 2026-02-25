package com.aprekek.ai_advent.agentic_app.data.state

import app.cash.sqldelight.db.SqlDriver

interface SqlDriverFactory {
    fun createDriver(): SqlDriver
}
