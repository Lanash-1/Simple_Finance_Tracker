package com.codigitech.ft.fakes

import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.AccountWithBalance
import com.codigitech.ft.domain.model.Budget
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.model.MonthTotals
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.PeriodTotals
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionFilter
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.model.Transfer
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.BudgetRepository
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.DateProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate

class FixedDate(var date: LocalDate) : DateProvider {
    override fun today(): LocalDate = date
}

class FakeTransactionRepository : TransactionRepository {
    val items = MutableStateFlow<List<Transaction>>(emptyList())
    private var nextId = 1L
    var transactionDepth = 0

    override fun observeRecent(limit: Long): Flow<List<Transaction>> = items.map { l -> l.filter { it.type != TransactionType.TRANSFER_IN }.take(limit.toInt()) }
    override fun observeFiltered(filter: TransactionFilter): Flow<List<Transaction>> = items.map { l ->
        if (filter.accountId == null) l.filter { it.type != TransactionType.TRANSFER_IN } else l.filter { it.accountId == filter.accountId }
    }
    override fun observeByAccount(accountId: Long): Flow<List<Transaction>> = items.map { l -> l.filter { it.accountId == accountId } }
    override fun observeTotals(from: LocalDate, to: LocalDate): Flow<PeriodTotals> = items.map { l ->
        val inRange = l.filter { it.date in from..to }
        PeriodTotals(
            inRange.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            inRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
        )
    }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Transaction>> = items.map { l -> l.filter { it.date in from..to } }
    override fun observeCategoryTotals(from: LocalDate, to: LocalDate, type: TransactionType): Flow<List<CategoryTotal>> = items.map { l ->
        l.filter { it.date in from..to && it.type == type }.groupBy { it.categoryId }
            .map { (cat, txns) -> CategoryTotal(cat, txns.sumOf { it.amount }) }.sortedByDescending { it.total }
    }
    override fun observeMonthlyTotals(from: LocalDate, to: LocalDate): Flow<List<MonthTotals>> = items.map { l ->
        l.filter { it.date in from..to }.groupBy { LocalDate(it.date.year, it.date.month, 1) }.entries.sortedBy { it.key }.map { (m, txns) ->
            MonthTotals(m, txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }, txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
        }
    }

    override suspend fun getById(id: Long): Transaction? = items.value.firstOrNull { it.id == id }
    override suspend fun getTransfer(transferId: String): Transfer? {
        val legs = items.value.filter { it.transferId == transferId }
        val out = legs.firstOrNull { it.type == TransactionType.TRANSFER_OUT } ?: return null
        val inn = legs.firstOrNull { it.type == TransactionType.TRANSFER_IN } ?: return null
        return Transfer(transferId, out.accountId, inn.accountId, out.amount, out.date, out.note)
    }

    override suspend fun add(accountId: Long, categoryId: Long?, type: TransactionType, amount: Long, date: LocalDate, note: String?, recurringRuleId: Long?): Long {
        val id = nextId++
        items.value += Transaction(id, accountId, categoryId, type, amount, date, note, null, null, recurringRuleId)
        return id
    }

    override suspend fun update(id: Long, accountId: Long, categoryId: Long?, type: TransactionType, amount: Long, date: LocalDate, note: String?) {
        items.value = items.value.map { if (it.id == id) it.copy(accountId = accountId, categoryId = categoryId, type = type, amount = amount, date = date, note = note) else it }
    }

    override suspend fun delete(id: Long) { items.value = items.value.filter { it.id != id } }

    override suspend fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?): String {
        val tid = "t${nextId}"
        items.value += Transaction(nextId++, fromAccountId, null, TransactionType.TRANSFER_OUT, amount, date, note, tid, toAccountId, null)
        items.value += Transaction(nextId++, toAccountId, null, TransactionType.TRANSFER_IN, amount, date, note, tid, fromAccountId, null)
        return tid
    }

    override suspend fun updateTransfer(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?) {
        items.value = items.value.map {
            when {
                it.transferId != transferId -> it
                it.type == TransactionType.TRANSFER_OUT -> it.copy(accountId = fromAccountId, counterpartAccountId = toAccountId, amount = amount, date = date, note = note)
                else -> it.copy(accountId = toAccountId, counterpartAccountId = fromAccountId, amount = amount, date = date, note = note)
            }
        }
    }

    override suspend fun deleteTransfer(transferId: String) { items.value = items.value.filter { it.transferId != transferId } }
    override suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) {
        items.value = items.value.map { if (it.categoryId == oldCategoryId) it.copy(categoryId = newCategoryId) else it }
    }
    override suspend fun countByAccount(accountId: Long): Long = items.value.count { it.accountId == accountId || it.counterpartAccountId == accountId }.toLong()
    override suspend fun <T> inTransaction(block: suspend () -> T): T {
        transactionDepth++
        return try { block() } finally { transactionDepth-- }
    }
}

class FakeRecurringRuleRepository : RecurringRuleRepository {
    val rules = MutableStateFlow<List<RecurringRule>>(emptyList())
    private var nextId = 1L

    fun seed(rule: RecurringRule) { rules.value += rule }

    override fun observeAll(): Flow<List<RecurringRule>> = rules
    override suspend fun getById(id: Long): RecurringRule? = rules.value.firstOrNull { it.id == id }
    override suspend fun getDue(onOrBefore: LocalDate): List<RecurringRule> = rules.value.filter { it.isActive && it.nextDueDate <= onOrBefore }
    override suspend fun nextActiveDueDate(): LocalDate? = rules.value.filter { it.isActive }.minOfOrNull { it.nextDueDate }
    override suspend fun add(amount: Long, accountId: Long, categoryId: Long?, type: TransactionType, frequency: Frequency, startDate: LocalDate, note: String?): Long {
        val id = nextId++
        rules.value += RecurringRule(id, amount, accountId, categoryId, type, frequency, startDate, startDate, true, note)
        return id
    }
    override suspend fun update(id: Long, amount: Long, accountId: Long, categoryId: Long?, type: TransactionType, frequency: Frequency, startDate: LocalDate, nextDueDate: LocalDate, note: String?) {
        rules.value = rules.value.map { if (it.id == id) it.copy(amount = amount, accountId = accountId, categoryId = categoryId, type = type, frequency = frequency, startDate = startDate, nextDueDate = nextDueDate, note = note) else it }
    }
    override suspend fun setNextDueDate(id: Long, date: LocalDate) { rules.value = rules.value.map { if (it.id == id) it.copy(nextDueDate = date) else it } }
    override suspend fun setActive(id: Long, active: Boolean, nextDueDate: LocalDate) {
        rules.value = rules.value.map { if (it.id == id) it.copy(isActive = active, nextDueDate = nextDueDate) else it }
    }
    override suspend fun delete(id: Long) { rules.value = rules.value.filter { it.id != id } }
    override suspend fun pauseByAccount(accountId: Long) { rules.value = rules.value.map { if (it.accountId == accountId) it.copy(isActive = false) else it } }
    override suspend fun deleteByAccount(accountId: Long) { rules.value = rules.value.filter { it.accountId != accountId } }
    override suspend fun reassignCategory(oldCategoryId: Long, newCategoryId: Long) {
        rules.value = rules.value.map { if (it.categoryId == oldCategoryId) it.copy(categoryId = newCategoryId) else it }
    }
}

class FakeAccountRepository : AccountRepository {
    val accounts = MutableStateFlow<List<Account>>(emptyList())
    private var nextId = 1L
    override fun observeAll(): Flow<List<Account>> = accounts
    override fun observeActive(): Flow<List<Account>> = accounts.map { l -> l.filter { !it.isArchived } }
    override fun observeBalances(): Flow<List<AccountWithBalance>> = accounts.map { l -> l.map { AccountWithBalance(it, it.initialBalance) } }
    override suspend fun getById(id: Long): Account? = accounts.value.firstOrNull { it.id == id }
    override suspend fun add(name: String, type: AccountType, initialBalance: Long, createdAt: LocalDate): Long {
        val id = nextId++
        accounts.value += Account(id, name, type, initialBalance, createdAt, false)
        return id
    }
    override suspend fun update(id: Long, name: String, type: AccountType, initialBalance: Long) {
        accounts.value = accounts.value.map { if (it.id == id) it.copy(name = name, type = type, initialBalance = initialBalance) else it }
    }
    override suspend fun setArchived(id: Long, archived: Boolean) { accounts.value = accounts.value.map { if (it.id == id) it.copy(isArchived = archived) else it } }
    override suspend fun deleteHard(id: Long) { accounts.value = accounts.value.filter { it.id != id } }
    override suspend fun count(): Long = accounts.value.size.toLong()
}

class FakeBudgetRepository : BudgetRepository {
    val budgets = MutableStateFlow<List<Budget>>(emptyList())
    override fun observeAll(): Flow<List<Budget>> = budgets
    override suspend fun set(categoryId: Long, monthlyLimit: Long) {
        budgets.value = budgets.value.filter { it.categoryId != categoryId } + Budget(categoryId, monthlyLimit)
    }
    override suspend fun remove(categoryId: Long) { budgets.value = budgets.value.filter { it.categoryId != categoryId } }
}
