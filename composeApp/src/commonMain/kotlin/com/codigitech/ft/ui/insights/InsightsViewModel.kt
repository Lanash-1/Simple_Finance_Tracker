package com.codigitech.ft.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.BudgetProgress
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.model.MonthTotals
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.BudgetRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.BudgetProgressCalculator
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.domain.usecase.MonthRange
import com.codigitech.ft.domain.usecase.SetBudgetUseCase
import com.codigitech.ft.ui.dashboard.CategoryShare
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus

data class Highlight(val label: String, val value: String, val detail: String? = null)

data class InsightsState(
    val loading: Boolean = true,
    val month: LocalDate? = null,
    val isCurrentMonth: Boolean = true,
    /** Always [TREND_MONTHS] entries ending at the selected month, zero-filled. */
    val trend: List<MonthTotals> = emptyList(),
    val totals: MonthTotals? = null,
    val spending: List<CategoryShare> = emptyList(),
    val income: List<CategoryShare> = emptyList(),
    val budgets: List<BudgetProgress> = emptyList(),
    /** Expense categories that do not have a budget yet. */
    val unbudgeted: List<Category> = emptyList(),
    val highlights: List<Highlight> = emptyList(),
    val categoriesById: Map<Long, Category> = emptyMap(),
) {
    companion object { const val TREND_MONTHS = 6 }
}

private data class MonthSlice(
    val month: LocalDate,
    val trend: List<MonthTotals>,
    val spend: List<CategoryTotal>,
    val earned: List<CategoryTotal>,
    val rows: List<Transaction>,
)

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModel(
    private val transactions: TransactionRepository,
    categories: CategoryRepository,
    budgets: BudgetRepository,
    private val setBudget: SetBudgetUseCase,
    dates: DateProvider,
) : ViewModel() {
    private val today = dates.today()
    private val currentMonth = LocalDate(today.year, today.month, 1)
    private val _month = MutableStateFlow(currentMonth)

    private val slice = _month.flatMapLatest { month ->
        val (from, to) = MonthRange.of(month)
        val trendFrom = month.minus((InsightsState.TREND_MONTHS - 1).toLong(), DateTimeUnit.MONTH)
        combine(
            transactions.observeMonthlyTotals(trendFrom, to),
            transactions.observeCategoryTotals(from, to, TransactionType.EXPENSE),
            transactions.observeCategoryTotals(from, to, TransactionType.INCOME),
            transactions.observeBetween(from, to),
        ) { trend, spend, earned, rows -> MonthSlice(month, fill(trend, trendFrom), spend, earned, rows) }
    }

    val state: StateFlow<InsightsState> = combine(slice, categories.observeAll(), budgets.observeAll()) { s, cats, allBudgets ->
        val byId = cats.associateBy { it.id }
        val progress = BudgetProgressCalculator.calculate(allBudgets, cats, s.spend)
        val budgeted = allBudgets.map { it.categoryId }.toSet()
        InsightsState(
            loading = false,
            month = s.month,
            isCurrentMonth = s.month == currentMonth,
            trend = s.trend,
            totals = s.trend.lastOrNull(),
            spending = shares(s.spend, byId),
            income = shares(s.earned, byId),
            budgets = progress,
            unbudgeted = cats.filter { it.type.name == "EXPENSE" && !it.isDefault && it.id !in budgeted },
            highlights = highlights(s, byId),
            categoriesById = byId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())

    fun previousMonth() = _month.update { it.minus(1, DateTimeUnit.MONTH) }
    fun nextMonth() = _month.update { if (it < currentMonth) it.plus(1, DateTimeUnit.MONTH) else it }
    fun resetMonth() = _month.update { currentMonth }
    fun selectTrendIndex(index: Int) {
        val target = state.value.trend.getOrNull(index)?.month ?: return
        if (target <= currentMonth) _month.value = target
    }

    fun saveBudget(categoryId: Long, limitMinor: Long) {
        viewModelScope.launch { setBudget(categoryId, limitMinor) }
    }

    private fun fill(actual: List<MonthTotals>, from: LocalDate): List<MonthTotals> {
        val byMonth = actual.associateBy { it.month }
        return (0 until InsightsState.TREND_MONTHS).map { i ->
            val m = from.plus(i.toLong(), DateTimeUnit.MONTH)
            byMonth[m] ?: MonthTotals(m, 0, 0)
        }
    }

    private fun shares(totals: List<CategoryTotal>, byId: Map<Long, Category>): List<CategoryShare> {
        val sum = totals.sumOf { it.total }.coerceAtLeast(1)
        return totals.map { CategoryShare(it.categoryId?.let(byId::get), it.total, it.total.toFloat() / sum) }
    }

    private fun highlights(s: MonthSlice, byId: Map<Long, Category>): List<Highlight> {
        val expenses = s.rows.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return emptyList()
        val (from, to) = MonthRange.of(s.month)
        val lastDay = if (s.month == currentMonth) today else to
        val days = (from.daysUntil(lastDay) + 1).coerceAtLeast(1)
        val spent = expenses.sumOf { it.amount }
        val largest = expenses.maxBy { it.amount }
        val busiest = expenses.groupBy { it.date }.maxBy { it.value.sumOf { t -> t.amount } }
        return listOf(
            Highlight("Average per day", com.codigitech.ft.domain.model.Money.format(spent / days), "over $days days"),
            Highlight(
                "Largest expense",
                com.codigitech.ft.domain.model.Money.format(largest.amount),
                largest.note?.takeIf { it.isNotBlank() } ?: largest.categoryId?.let { byId[it]?.name } ?: "Uncategorized",
            ),
            Highlight("Busiest day", com.codigitech.ft.domain.model.Money.format(busiest.value.sumOf { it.amount }), "${busiest.key.day} ${busiest.key.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} · ${busiest.value.size} entries"),
            Highlight("Transactions", s.rows.size.toString(), "${expenses.size} expenses"),
        )
    }
}
