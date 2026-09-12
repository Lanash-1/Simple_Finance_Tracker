package com.codigitech.ft.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.AccountWithBalance
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.model.PeriodTotals
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.BudgetRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.BudgetProgressCalculator
import com.codigitech.ft.domain.usecase.CalculateBalanceUseCase
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.domain.usecase.MonthRange
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** One category's slice of the month's spending. */
data class CategoryShare(val category: Category?, val total: Long, val share: Float)

data class UpcomingRule(val rule: RecurringRule, val category: Category?, val account: Account?, val daysAway: Int)

data class DashboardState(
    val loading: Boolean = true,
    val today: LocalDate? = null,
    val month: LocalDate? = null,
    val isCurrentMonth: Boolean = true,
    val totalBalance: Long = 0,
    val accounts: List<AccountWithBalance> = emptyList(),
    val totals: PeriodTotals = PeriodTotals(0, 0),
    val spending: List<CategoryShare> = emptyList(),
    val recent: List<Transaction> = emptyList(),
    val upcoming: List<UpcomingRule> = emptyList(),
    val budgetsOver: Int = 0,
    val budgetCount: Int = 0,
    val accountsById: Map<Long, Account> = emptyMap(),
    val categoriesById: Map<Long, Category> = emptyMap(),
)

private data class MonthSlice(val month: LocalDate, val totals: PeriodTotals, val spend: List<CategoryTotal>, val recent: List<Transaction>)

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    accounts: AccountRepository,
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    rules: RecurringRuleRepository,
    budgets: BudgetRepository,
    calculateBalance: CalculateBalanceUseCase,
    dates: DateProvider,
) : ViewModel() {
    private val today = dates.today()
    private val currentMonth = LocalDate(today.year, today.month, 1)
    private val _month = MutableStateFlow(currentMonth)

    private val monthSlice = _month.flatMapLatest { month ->
        val (from, to) = MonthRange.of(month)
        combine(
            transactions.observeTotals(from, to),
            transactions.observeCategoryTotals(from, to, TransactionType.EXPENSE),
            transactions.observeRecent(RECENT_LIMIT),
        ) { totals, spend, recent -> MonthSlice(month, totals, spend, recent) }
    }

    val state: StateFlow<DashboardState> = combine(
        monthSlice,
        accounts.observeBalances(),
        categories.observeAll(),
        rules.observeAll(),
        budgets.observeAll(),
    ) { slice, balances, cats, allRules, allBudgets ->
        val catsById = cats.associateBy { it.id }
        val spendTotal = slice.spend.sumOf { it.total }.coerceAtLeast(1)
        val progress = BudgetProgressCalculator.calculate(allBudgets, cats, slice.spend)
        DashboardState(
            loading = false,
            today = today,
            month = slice.month,
            isCurrentMonth = slice.month == currentMonth,
            totalBalance = calculateBalance(balances),
            accounts = balances.filter { !it.account.isArchived },
            totals = slice.totals,
            spending = slice.spend.map { CategoryShare(it.categoryId?.let(catsById::get), it.total, it.total.toFloat() / spendTotal) },
            recent = slice.recent,
            upcoming = allRules.filter { it.isActive }
                .map { UpcomingRule(it, it.categoryId?.let(catsById::get), balances.firstOrNull { b -> b.account.id == it.accountId }?.account, today.daysUntil(it.nextDueDate)) }
                .filter { it.daysAway in 0..UPCOMING_WINDOW_DAYS }
                .sortedBy { it.daysAway }
                .take(3),
            budgetsOver = progress.count { it.isOver },
            budgetCount = progress.size,
            accountsById = balances.associate { it.account.id to it.account },
            categoriesById = catsById,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    fun previousMonth() = _month.update { it.minus(1, DateTimeUnit.MONTH) }
    fun nextMonth() = _month.update { if (it < currentMonth) it.plus(1, DateTimeUnit.MONTH) else it }
    fun resetMonth() = _month.update { currentMonth }

    private companion object {
        const val RECENT_LIMIT = 8L
        const val UPCOMING_WINDOW_DAYS = 14
    }
}
