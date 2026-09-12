package com.codigitech.ft.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.data.db.toDb
import com.codigitech.ft.data.db.toDomain
import com.codigitech.ft.data.db.toEpochDay
import com.codigitech.ft.data.db.toLocalDate
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class RecurringRuleRepositoryImpl(private val db: FinanceDatabase, private val exec: DbExecutor) : RecurringRuleRepository {
    private val q get() = db.recurringRuleQueries

    override fun observeAll(): Flow<List<RecurringRule>> =
        q.selectAll().asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): RecurringRule? = exec.run { q.selectById(id).executeAsOneOrNull()?.toDomain() }

    override suspend fun getDue(onOrBefore: LocalDate): List<RecurringRule> =
        exec.run { q.selectDue(onOrBefore.toEpochDay()).executeAsList().map { it.toDomain() } }

    override suspend fun nextActiveDueDate(): LocalDate? =
        exec.run { q.selectNextActiveDueDate().executeAsOneOrNull()?.MIN?.toLocalDate() }

    override suspend fun add(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        frequency: Frequency,
        startDate: LocalDate,
        note: String?,
    ): Long = exec.run {
        q.insert(amount, accountId, categoryId, type.name, frequency.name, startDate.toEpochDay(), startDate.toEpochDay(), note)
        q.lastInsertRowId().executeAsOne()
    }

    override suspend fun update(
        id: Long,
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        frequency: Frequency,
        startDate: LocalDate,
        nextDueDate: LocalDate,
        note: String?,
    ) = exec.exec {
        q.update(amount, accountId, categoryId, type.name, frequency.name, startDate.toEpochDay(), nextDueDate.toEpochDay(), note, id)
    }

    override suspend fun setNextDueDate(id: Long, date: LocalDate) = exec.exec { q.setNextDueDate(date.toEpochDay(), id) }

    override suspend fun setActive(id: Long, active: Boolean, nextDueDate: LocalDate) =
        exec.exec { q.setActive(active.toDb(), nextDueDate.toEpochDay(), id) }

    override suspend fun delete(id: Long) = exec.exec { q.deleteById(id) }

    override suspend fun pauseByAccount(accountId: Long) = exec.exec { q.pauseByAccount(accountId) }

    override suspend fun deleteByAccount(accountId: Long) = exec.exec { q.deleteByAccount(accountId) }

    override suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) =
        exec.exec { q.reassignCategory(newCategoryId, oldCategoryId) }
}
