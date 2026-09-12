package com.codigitech.ft.domain

import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.usecase.AddTransactionUseCase
import com.codigitech.ft.domain.usecase.DeleteAccountUseCase
import com.codigitech.ft.domain.usecase.DeleteTransactionUseCase
import com.codigitech.ft.domain.usecase.Result
import com.codigitech.ft.domain.usecase.TransferUseCase
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.fakes.FakeAccountRepository
import com.codigitech.ft.fakes.FakeRecurringRuleRepository
import com.codigitech.ft.fakes.FakeTransactionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class TransactionUseCaseTest {
    private val date = LocalDate(2026, 9, 5)

    @Test
    fun rejectsNonPositiveAmounts() = runTest {
        val repo = FakeTransactionRepository()
        assertIs<Result.Failure>(AddTransactionUseCase(repo)(1, 1, TransactionType.EXPENSE, 0, date, null))
        assertIs<Result.Failure>(AddTransactionUseCase(repo)(1, 1, TransactionType.EXPENSE, -5, date, null))
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun blankNoteIsStoredAsNull() = runTest {
        val repo = FakeTransactionRepository()
        AddTransactionUseCase(repo)(1, 1, TransactionType.INCOME, 100, date, "   ")
        assertEquals(null, repo.items.value.single().note)
    }

    @Test
    fun transferToSameAccountIsRejected() = runTest {
        val repo = FakeTransactionRepository()
        val result = TransferUseCase(repo).add(1, 1, 100, date, null)
        assertIs<Result.Failure>(result)
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun transferCreatesLinkedPairThatNetsToZero() = runTest {
        val repo = FakeTransactionRepository()
        TransferUseCase(repo).add(1, 2, 5000, date, "to bank")
        val legs = repo.items.value
        assertEquals(2, legs.size)
        assertEquals(0, legs.sumOf { it.signedAmount })
        assertEquals(-5000, legs.first { it.accountId == 1L }.signedAmount)
        assertEquals(5000, legs.first { it.accountId == 2L }.signedAmount)
        assertTrue(legs.all { it.type.isTransfer })
    }

    @Test
    fun deletingOneTransferLegRemovesBoth() = runTest {
        val repo = FakeTransactionRepository()
        TransferUseCase(repo).add(1, 2, 5000, date, null)
        DeleteTransactionUseCase(repo)(repo.items.value.first().id)
        assertTrue(repo.items.value.isEmpty())
    }

    @Test
    fun deletingAccountWithHistoryArchivesInstead() = runTest {
        val accounts = FakeAccountRepository()
        val txns = FakeTransactionRepository()
        val cash = accounts.add("Cash", AccountType.CASH, 0, date)
        val bank = accounts.add("Bank", AccountType.BANK, 0, date)
        txns.add(cash, 1, TransactionType.EXPENSE, 100, date, null)

        val rules = FakeRecurringRuleRepository()
        rules.add(500, cash, 1, TransactionType.EXPENSE, Frequency.MONTHLY, date, "on cash")
        rules.add(700, bank, 1, TransactionType.EXPENSE, Frequency.MONTHLY, date, "on bank")

        val useCase = DeleteAccountUseCase(accounts, txns, rules)
        assertEquals(DeleteAccountUseCase.Outcome.ARCHIVED, useCase(cash))
        assertEquals(DeleteAccountUseCase.Outcome.DELETED, useCase(bank))
        assertEquals(listOf(cash), accounts.accounts.value.map { it.id })
        assertTrue(accounts.accounts.value.single().isArchived)
        // Rules on the archived account are paused, rules on the deleted account are gone.
        val remaining = rules.rules.value
        assertEquals(listOf("on cash"), remaining.map { it.note })
        assertTrue(remaining.none { it.isActive })
    }
}
