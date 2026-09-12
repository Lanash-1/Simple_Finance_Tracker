package com.codigitech.ft.domain

import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.usecase.ProcessDueRecurringUseCase
import com.codigitech.ft.domain.usecase.ToggleRecurringRuleUseCase
import com.codigitech.ft.fakes.FakeRecurringRuleRepository
import com.codigitech.ft.fakes.FakeTransactionRepository
import com.codigitech.ft.fakes.FixedDate
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecurringTest {
    private fun rule(next: LocalDate, freq: Frequency = Frequency.MONTHLY, active: Boolean = true) = RecurringRule(
        id = 1, amount = 1500000, accountId = 1, categoryId = 5, type = TransactionType.EXPENSE,
        frequency = freq, startDate = next, nextDueDate = next, isActive = active, note = "Rent",
    )

    @Test
    fun monthlyNextHandlesShortMonths() {
        assertEquals(LocalDate(2026, 2, 28), Frequency.MONTHLY.next(LocalDate(2026, 1, 31)))
        assertEquals(LocalDate(2026, 3, 1), Frequency.DAILY.next(LocalDate(2026, 2, 28)))
        assertEquals(LocalDate(2026, 1, 8), Frequency.WEEKLY.next(LocalDate(2026, 1, 1)))
    }

    @Test
    fun monthlyAnchoredToStartDayDoesNotDrift() {
        val anchor = LocalDate(2026, 1, 31)
        assertEquals(LocalDate(2026, 2, 28), Frequency.MONTHLY.next(anchor, anchor))
        assertEquals(LocalDate(2026, 3, 31), Frequency.MONTHLY.next(LocalDate(2026, 2, 28), anchor))
        assertEquals(LocalDate(2026, 4, 30), Frequency.MONTHLY.next(LocalDate(2026, 3, 31), anchor))
        // Daily / weekly ignore the anchor.
        assertEquals(LocalDate(2026, 3, 1), Frequency.DAILY.next(LocalDate(2026, 2, 28), anchor))
        assertEquals(LocalDate(2026, 3, 7), Frequency.WEEKLY.next(LocalDate(2026, 2, 28), anchor))
    }

    @Test
    fun processingKeepsTheAnchoredDayOfMonth() = runTest {
        val rules = FakeRecurringRuleRepository().apply { seed(rule(LocalDate(2026, 1, 31))) }
        val txns = FakeTransactionRepository()
        ProcessDueRecurringUseCase(rules, txns, FixedDate(LocalDate(2026, 4, 1)))()
        assertEquals(
            listOf(LocalDate(2026, 1, 31), LocalDate(2026, 2, 28), LocalDate(2026, 3, 31)),
            txns.items.value.map { it.date },
        )
        assertEquals(LocalDate(2026, 4, 30), rules.getById(1)!!.nextDueDate)
    }

    @Test
    fun generatesEveryMissedOccurrenceAndAdvancesPastToday() = runTest {
        val rules = FakeRecurringRuleRepository().apply { seed(rule(LocalDate(2026, 6, 1))) }
        val txns = FakeTransactionRepository()
        val today = FixedDate(LocalDate(2026, 9, 5))
        val outcome = ProcessDueRecurringUseCase(rules, txns, today)()

        assertEquals(4, outcome.generated) // Jun, Jul, Aug, Sep
        assertEquals(listOf(LocalDate(2026, 6, 1), LocalDate(2026, 7, 1), LocalDate(2026, 8, 1), LocalDate(2026, 9, 1)), txns.items.value.map { it.date })
        assertTrue(txns.items.value.all { it.recurringRuleId == 1L && it.amount == 1500000L })
        assertEquals(LocalDate(2026, 10, 1), rules.getById(1)!!.nextDueDate)
    }

    @Test
    fun secondRunIsIdempotent() = runTest {
        val rules = FakeRecurringRuleRepository().apply { seed(rule(LocalDate(2026, 9, 1))) }
        val txns = FakeTransactionRepository()
        val useCase = ProcessDueRecurringUseCase(rules, txns, FixedDate(LocalDate(2026, 9, 5)))
        useCase(); useCase()
        assertEquals(1, txns.items.value.size)
    }

    @Test
    fun futureAndPausedRulesAreSkipped() = runTest {
        val rules = FakeRecurringRuleRepository().apply {
            seed(rule(LocalDate(2026, 9, 6)))
            seed(rule(LocalDate(2026, 1, 1), active = false).copy(id = 2))
        }
        val txns = FakeTransactionRepository()
        val outcome = ProcessDueRecurringUseCase(rules, txns, FixedDate(LocalDate(2026, 9, 5)))()
        assertEquals(0, outcome.generated)
    }

    @Test
    fun resumingSkipsMissedDates() = runTest {
        val rules = FakeRecurringRuleRepository().apply { seed(rule(LocalDate(2026, 3, 10), active = false)) }
        ToggleRecurringRuleUseCase(rules, FixedDate(LocalDate(2026, 9, 5)))(1, active = true)
        val resumed = rules.getById(1)!!
        assertTrue(resumed.isActive)
        assertEquals(LocalDate(2026, 9, 10), resumed.nextDueDate)
    }

    @Test
    fun resumingOnDueDateKeepsIt() {
        assertEquals(
            LocalDate(2026, 9, 5),
            ToggleRecurringRuleUseCase.skipToOnOrAfter(LocalDate(2026, 9, 5), Frequency.WEEKLY, LocalDate(2026, 9, 5)),
        )
    }
}
