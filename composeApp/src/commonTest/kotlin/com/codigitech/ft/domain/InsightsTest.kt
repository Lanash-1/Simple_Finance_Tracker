package com.codigitech.ft.domain

import com.codigitech.ft.domain.model.Budget
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.TransactionKind
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.model.CategoryTotal
import com.codigitech.ft.domain.usecase.BudgetProgressCalculator
import com.codigitech.ft.domain.usecase.DeleteTransactionUseCase
import com.codigitech.ft.domain.usecase.DeletedEntry
import com.codigitech.ft.domain.usecase.RestoreTransactionUseCase
import com.codigitech.ft.domain.usecase.SetBudgetUseCase
import com.codigitech.ft.fakes.FakeBudgetRepository
import com.codigitech.ft.fakes.FakeTransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InsightsTest {
    private val day = LocalDate(2026, 9, 5)
    private fun cat(id: Long, type: CategoryType = CategoryType.EXPENSE) = Category(id, "C$id", "x", "#000000", type, false, false)

    @Test
    fun budgetProgressSortsOverBudgetFirstAndIgnoresIncomeCategories() {
        val cats = listOf(cat(1), cat(2), cat(3, CategoryType.INCOME))
        val budgets = listOf(Budget(1, 1000), Budget(2, 1000), Budget(3, 1000))
        val spend = listOf(CategoryTotal(1, 400), CategoryTotal(2, 1200), CategoryTotal(null, 50))
        val progress = BudgetProgressCalculator.calculate(budgets, cats, spend)
        assertEquals(listOf(2L, 1L), progress.map { it.category.id })
        assertTrue(progress[0].isOver)
        assertEquals(-200, progress[0].remaining)
        assertEquals(0.4f, progress[1].fraction)
    }

    @Test
    fun zeroLimitRemovesBudget() = runTest {
        val repo = FakeBudgetRepository()
        val set = SetBudgetUseCase(repo)
        set(1, 500)
        assertEquals(1, repo.budgets.value.size)
        set(1, 0)
        assertTrue(repo.budgets.value.isEmpty())
    }

    @Test
    fun deleteReturnsSnapshotAndRestorePutsTransferBack() = runTest {
        val repo = FakeTransactionRepository()
        repo.addTransfer(1, 2, 700, day, "move")
        val legId = repo.items.value.first().id
        val snapshot = DeleteTransactionUseCase(repo)(legId)
        assertIs<DeletedEntry.Pair>(snapshot)
        assertTrue(repo.items.value.isEmpty())

        RestoreTransactionUseCase(repo)(snapshot)
        val restored = repo.items.value
        assertEquals(2, restored.size)
        assertEquals(setOf(TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN), restored.map { it.type }.toSet())
        assertEquals(700, restored.first().amount)
    }

    @Test
    fun deleteSingleAndRestoreKeepsRecurringLink() = runTest {
        val repo = FakeTransactionRepository()
        val id = repo.add(1, 9, TransactionType.EXPENSE, 300, day, "rent", recurringRuleId = 42)
        val snapshot = DeleteTransactionUseCase(repo)(id)
        assertIs<DeletedEntry.Single>(snapshot)
        RestoreTransactionUseCase(repo)(snapshot)
        val back = repo.items.value.single()
        assertEquals(42, back.recurringRuleId)
        assertEquals(TransactionKind.EXPENSE, back.kind)
    }

    @Test
    fun monthlyTotalsGroupByCalendarMonth() = runTest {
        val repo = FakeTransactionRepository()
        repo.add(1, null, TransactionType.INCOME, 1000, LocalDate(2026, 8, 30), null)
        repo.add(1, null, TransactionType.EXPENSE, 250, LocalDate(2026, 9, 1), null)
        repo.add(1, null, TransactionType.EXPENSE, 250, LocalDate(2026, 9, 20), null)
        val months = repo.observeMonthlyTotals(LocalDate(2026, 8, 1), LocalDate(2026, 9, 30)).first()
        assertEquals(2, months.size)
        assertEquals(1000, months[0].income)
        assertEquals(500, months[1].expense)
        assertNotNull(months.firstOrNull { it.month == LocalDate(2026, 9, 1) })
    }
}
