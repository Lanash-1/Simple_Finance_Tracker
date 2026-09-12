package com.codigitech.ft.data.db

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

const val DATABASE_FILE_NAME = "finance_tracker.db"
