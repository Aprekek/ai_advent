package com.aprekek.ai_advent.agentic_app.data.state

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.aprekek.ai_advent.agentic_app.data.state.db.SessionHistoryDatabase
import java.io.File

class JvmSqlDriverFactory(
    private val dbPath: String
) : SqlDriverFactory {
    override fun createDriver(): SqlDriver {
        val dbFile = File(dbPath)
        dbFile.parentFile?.mkdirs()
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${dbFile.absolutePath}",
            schema = SessionHistoryDatabase.Schema
        )
    }
}
