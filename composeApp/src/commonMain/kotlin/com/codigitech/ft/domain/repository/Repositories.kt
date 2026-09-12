package com.codigitech.ft.domain.repository

import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.AccountWithBalance
import com.codigitech.ft.domain.model.Budget
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.model.MonthTotals
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.PeriodTotals
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionFilter
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.model.Transfer
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface AccountRepository {
    fun observeAll(): Flow<List<Account>>
    fun observeActive(): Flow<List<Account>>
    fun observeBalances(): Flow<List<AccountWithBalance>>
    suspend fun getById(id: Long): Account?
    suspend fun add(name: String, type: AccountType, initialBalance: Long, createdAt: LocalDate): Long
    suspend fun update(id: Long, name: String, type: AccountType, initialBalance: Long)
    suspend fun setArchived(id: Long, archived: Boolean)
    suspend fun deleteHard(id: Long)
    suspend fun count(): Long
}

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>
    suspend fun getAll(): List<Category>
    suspend fun getById(id: Long): Category?
    suspend fun getDefault(type: CategoryType): Category?
    suspend fun add(name: String, icon: String, colorHex: String, type: CategoryType, isCustom: Boolean, isDefault: Boolean): Long
    suspend fun update(id: Long, name: String, icon: String, colorHex: String)
    suspend fun delete(id: Long)
    suspend fun count(): Long
}

interface TransactionRepository {
    fun observeRecent(limit: Long): Flow<List<Transaction>>
    fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>>
    fun observeByAccount(accountId: Long): Flow<List<Transaction>>
    fun observeTotals(from: LocalDate, to: LocalDate): Flow<PeriodTotals>
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>>
    /** Per-category totals of one type in a period, largest first. Active accounts only. */
    fun observeCategoryTotals(from: LocalDate, to: LocalDate, type: TransactionType): Flow<List<CategoryTotal>>
    /** One entry per calendar month that has activity in the range, ascending. */
    fun observeMonthlyTotals(from: LocalDate, to: LocalDate): Flow<List<MonthTotals>>
    suspend fun getById(id: Long): Transaction?
    suspend fun getTransfer(transferId: String): Transfer?
    suspend fun add(
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        note: String?,
        recurringRuleId: Long? = null,
    ): Long
    suspend fun update(id: Long, accountId: Long, categoryId: Long?, type: TransactionType, amount: Long, date: LocalDate, note: String?)
    suspend fun delete(id: Long)
    suspend fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?): String
    suspend fun updateTransfer(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?)
    suspend fun deleteTransfer(transferId: String)
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)
    suspend fun countByAccount(accountId: Long): Long
    /** Runs [block] inside one DB transaction. */
    suspend fun <T> inTransaction(block: suspend () -> T): T
}

interface RecurringRuleRepository {
    fun observeAll(): Flow<List<RecurringRule>>
    suspend fun getById(id: Long): RecurringRule?
    suspend fun getDue(onOrBefore: LocalDate): List<RecurringRule>
    suspend fun nextActiveDueDate(): LocalDate?
    suspend fun add(
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        frequency: Frequency,
        startDate: LocalDate,
        note: String?,
    ): Long
    suspend fun update(
        id: Long,
        amount: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        frequency: Frequency,
        startDate: LocalDate,
        nextDueDate: LocalDate,
        note: String?,
    )
    suspend fun setNextDueDate(id: Long, date: LocalDate)
    suspend fun setActive(id: Long, active: Boolean, nextDueDate: LocalDate)
    suspend fun delete(id: Long)
    /** Pauses every rule that posts to [accountId] (used when the account is archived). */
    suspend fun pauseByAccount(accountId: Long)
    /** Removes every rule that posts to [accountId] (used when the account is hard-deleted). */
    suspend fun deleteByAccount(accountId: Long)
    /** Moves rules from a deleted category to its type's Uncategorized. */
    suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long)
}

interface BudgetRepository {
    fun observeAll(): Flow<List<Budget>>
    suspend fun set(categoryId: Long, monthlyLimit: Long)
    suspend fun remove(categoryId: Long)
}
