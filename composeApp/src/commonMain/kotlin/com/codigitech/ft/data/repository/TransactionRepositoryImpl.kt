package com.codigitech.ft.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.data.db.toDomain
import com.codigitech.ft.data.db.toEpochDay
import com.codigitech.ft.data.db.toLocalDate
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.model.MonthTotals
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.PeriodTotals
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionFilter
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.model.Transfer
import com.codigitech.ft.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlin.random.Random
import kotlin.time.Clock

class TransactionRepositoryImpl(private val db: FinanceDatabase, private val exec: DbExecutor) : TransactionRepository {
    private val q get() = db.transactionEntityQueries

    override fun observeRecent(limit: Long): Flow<List<Transaction>> =
        q.selectRecent(limit * 2).asFlow().mapToList(exec.dispatcher).map { rows ->
            rows.map { it.toDomain() }.collapseTransfers().take(limit.toInt())
        }

    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> =
        q.selectFiltered(
            accountId = filter.accountId,
            categoryId = filter.categoryId,
            fromDate = filter.fromDate?.toEpochDay(),
            toDate = filter.toDate?.toEpochDay(),
        ).asFlow().mapToList(exec.dispatcher).map { rows ->
            val list = rows.map { it.toDomain() }
            val query = filter.query.trim()
            list.asSequence()
                .filter { filter.kind == null || it.kind == filter.kind }
                .filter { query.isEmpty() || it.matches(query) }
                .toList()
                // With an account filter each transfer has exactly one leg in the result, so keep it as is.
                .let { if (filter.accountId == null) it.collapseTransfers() else it }
        }

    /**
     * A transfer is stored as two rows; in a list that spans all accounts the user should see it
     * once. Keep the outgoing leg (it names both sides) and drop the incoming one.
     */
    private fun List<Transaction>.collapseTransfers(): List<Transaction> = filter { it.type != TransactionType.TRANSFER_IN }

    private fun Transaction.matches(query: String): Boolean {
        if (note?.contains(query, ignoreCase = true) == true) return true
        val amountMinor = Money.parse(query) ?: return false
        return amount == amountMinor
    }

    override fun observeByAccount(accountId: Long): Flow<List<Transaction>> =
        q.selectByAccount(accountId).asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeTotals(from: LocalDate, to: LocalDate): Flow<PeriodTotals> =
        q.selectTotalsBetween(from.toEpochDay(), to.toEpochDay()).asFlow().mapToOne(exec.dispatcher)
            .map { PeriodTotals(income = it.income.toLong(), expense = it.expense.toLong()) }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>> =
        q.selectBetween(from.toEpochDay(), to.toEpochDay()).asFlow().mapToList(exec.dispatcher).map { rows -> rows.map { it.toDomain() } }

    override fun observeCategoryTotals(from: LocalDate, to: LocalDate, type: TransactionType): Flow<List<CategoryTotal>> =
        q.selectCategoryTotalsBetween(type.name, from.toEpochDay(), to.toEpochDay()).asFlow().mapToList(exec.dispatcher)
            .map { rows -> rows.map { CategoryTotal(it.categoryId, it.total ?: 0L) } }

    override fun observeMonthlyTotals(from: LocalDate, to: LocalDate): Flow<List<MonthTotals>> =
        q.selectMonthlyTotalsBetween(from.toEpochDay(), to.toEpochDay()).asFlow().mapToList(exec.dispatcher).map { rows ->
            rows.mapNotNull { row ->
                val ym = row.ym ?: return@mapNotNull null
                val year = ym.substringBefore('-').toIntOrNull() ?: return@mapNotNull null
                val month = ym.substringAfter('-').toIntOrNull() ?: return@mapNotNull null
                MonthTotals(LocalDate(year, month, 1), row.income.toLong(), row.expense.toLong())
            }
        }

    override suspend fun getById(id: Long): Transaction? = exec.run { q.selectById(id).executeAsOneOrNull()?.toDomain() }

    override suspend fun getTransfer(transferId: String): Transfer? = exec.run {
        val legs = q.selectByTransferId(transferId).executeAsList().map { it.toDomain() }
        val out = legs.firstOrNull { it.type == TransactionType.TRANSFER_OUT } ?: return@run null
        val inn = legs.firstOrNull { it.type == TransactionType.TRANSFER_IN } ?: return@run null
        Transfer(transferId, out.accountId, inn.accountId, out.amount, out.date, out.note)
    }

    override suspend fun add(
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        note: String?,
        recurringRuleId: Long?,
    ): Long = exec.run {
        q.insert(accountId, categoryId, type.name, amount, date.toEpochDay(), note, null, null, recurringRuleId, now())
        q.lastInsertRowId().executeAsOne()
    }

    override suspend fun update(id: Long, accountId: Long, categoryId: Long?, type: TransactionType, amount: Long, date: LocalDate, note: String?) =
        exec.exec { q.update(accountId, categoryId, type.name, amount, date.toEpochDay(), note, id) }

    override suspend fun delete(id: Long) = exec.exec { q.deleteById(id) }

    override suspend fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?): String {
        val transferId = newTransferId()
        exec.transaction {
            val ts = now()
            q.insert(fromAccountId, null, TransactionType.TRANSFER_OUT.name, amount, date.toEpochDay(), note, transferId, toAccountId, null, ts)
            q.insert(toAccountId, null, TransactionType.TRANSFER_IN.name, amount, date.toEpochDay(), note, transferId, fromAccountId, null, ts)
        }
        return transferId
    }

    override suspend fun updateTransfer(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?) {
        exec.transaction {
            val legs = q.selectByTransferId(transferId).executeAsList()
            val out = legs.firstOrNull { it.type == TransactionType.TRANSFER_OUT.name } ?: return@transaction
            val inn = legs.firstOrNull { it.type == TransactionType.TRANSFER_IN.name } ?: return@transaction
            q.updateTransferLeg(fromAccountId, toAccountId, amount, date.toEpochDay(), note, out.id)
            q.updateTransferLeg(toAccountId, fromAccountId, amount, date.toEpochDay(), note, inn.id)
        }
    }

    override suspend fun deleteTransfer(transferId: String) = exec.exec { q.deleteByTransferId(transferId) }

    override suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) =
        exec.exec { q.reassignCategory(newCategoryId, oldCategoryId) }

    override suspend fun countByAccount(accountId: Long): Long = exec.run { q.countByAccount(accountId, accountId).executeAsOne() }

    override suspend fun <T> inTransaction(block: suspend () -> T): T = exec.transaction(block)

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private fun newTransferId(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        val random = (1..12).map { alphabet[Random.nextInt(alphabet.length)] }.joinToString("")
        return "${now()}-$random"
    }
}
