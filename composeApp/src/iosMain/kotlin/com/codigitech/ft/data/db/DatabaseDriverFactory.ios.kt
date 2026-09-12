package com.codigitech.ft.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.codigitech.ft.db.FinanceDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver = NativeSqliteDriver(FinanceDatabase.Schema, DATABASE_FILE_NAME)
}
