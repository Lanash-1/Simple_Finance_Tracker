package com.codigitech.ft.platform

import com.codigitech.ft.Brand
import com.codigitech.ft.data.db.DatabaseSeeder
import com.codigitech.ft.data.db.toEpochDay
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.usecase.ProcessDueRecurringUseCase
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Runs on every app launch / foreground: seeds the DB if needed, generates missed
 * recurring transactions, posts one summary notification, and (re)schedules the
 * best-effort OS reminder for the next due date.
 */
class RecurringSyncCoordinator(
    private val seeder: DatabaseSeeder,
    private val processDue: ProcessDueRecurringUseCase,
    private val rules: RecurringRuleRepository,
    private val notifier: Notifier,
) {
    private val mutex = Mutex()
    var scheduler: RecurringScheduler? = null

    suspend fun sync(notify: Boolean = true): Int = mutex.withLock {
        seeder.seedIfEmpty()
        val outcome = processDue()
        if (notify && outcome.generated > 0) {
            val body = if (outcome.generated == 1) "1 recurring transaction added"
            else "${outcome.generated} recurring transactions added"
            notifier.notify(Brand.APP_NAME, body)
        }
        rescheduleReminder()
        outcome.generated
    }

    suspend fun rescheduleReminder() {
        scheduler?.schedule(rules.nextActiveDueDate()?.toEpochDay())
    }
}
