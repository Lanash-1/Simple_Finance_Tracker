package com.codigitech.ft.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codigitech.ft.data.db.DatabaseSeeder
import com.codigitech.ft.data.db.DbExecutor
import com.codigitech.ft.data.repository.AccountRepositoryImpl
import com.codigitech.ft.data.repository.BudgetRepositoryImpl
import com.codigitech.ft.data.repository.CategoryRepositoryImpl
import com.codigitech.ft.data.repository.RecurringRuleRepositoryImpl
import com.codigitech.ft.data.repository.TransactionRepositoryImpl
import com.codigitech.ft.db.FinanceDatabase
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.TransactionFilter
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.usecase.DeleteAccountUseCase
import com.codigitech.ft.domain.usecase.DeleteCategoryUseCase
import com.codigitech.ft.domain.usecase.Result
import com.codigitech.ft.fakes.FixedDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DatabaseTest {
    private lateinit var db: FinanceDatabase
    private lateinit var accounts: AccountRepositoryImpl
    private lateinit var categories: CategoryRepositoryImpl
    private lateinit var transactions: TransactionRepositoryImpl
    private lateinit var budgets: BudgetRepositoryImpl
    private lateinit var rules: RecurringRuleRepositoryImpl
    private val today = LocalDate(2026, 9, 5)

    @BeforeTest
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanceDatabase.Schema.create(driver)
        db = FinanceDatabase(driver)
        val exec = DbExecutor(db)
        accounts = AccountRepositoryImpl(db, exec)
        categories = CategoryRepositoryImpl(db, exec)
        transactions = TransactionRepositoryImpl(db, exec)
        budgets = BudgetRepositoryImpl(db, exec)
        rules = RecurringRuleRepositoryImpl(db, exec)
    }

    @Test
    fun seederCreatesCategoriesAndCashAccountOnce() = runTest {
        val seeder = DatabaseSeeder(accounts, categories, FixedDate(today))
        seeder.seedIfEmpty(); seeder.seedIfEmpty()
        assertEquals(15, categories.count()) // 8 expense + 5 income + 2 uncategorized
        assertEquals(1, accounts.count())
        assertNotNull(categories.getDefault(CategoryType.EXPENSE))
        assertNotNull(categories.getDefault(CategoryType.INCOME))
    }

    @Test
    fun balancesAccountForTransfersWithoutTouchingTotals() = runTest {
        val cash = accounts.add("Cash", AccountType.CASH, 10000, today)
        val bank = accounts.add("Bank", AccountType.BANK, 0, today)
        transactions.add(cash, null, TransactionType.INCOME, 50000, today, null)
        transactions.add(cash, null, TransactionType.EXPENSE, 2000, today, null)
        transactions.addTransfer(cash, bank, 30000, today, null)

        val balances = accounts.observeBalances().first().associate { it.account.name to it.balance }
        assertEquals(28000, balances["Cash"]) // 10000 + 50000 - 2000 - 30000
        assertEquals(30000, balances["Bank"])

        val totals = transactions.observeTotals(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30)).first()
        assertEquals(50000, totals.income)
        assertEquals(2000, totals.expense)
        assertEquals(48000, totals.net)
    }

    @Test
    fun archivedAccountsAreExcludedFromMonthlyTotals() = runTest {
        val cash = accounts.add("Cash", AccountType.CASH, 0, today)
        transactions.add(cash, null, TransactionType.EXPENSE, 700, today, null)
        accounts.setArchived(cash, true)
        assertEquals(0, transactions.observeTotals(today, today).first().expense)
    }

    @Test
    fun transferRoundTripAndDelete() = runTest {
        val a = accounts.add("A", AccountType.CASH, 0, today)
        val b = accounts.add("B", AccountType.BANK, 0, today)
        val id = transactions.addTransfer(a, b, 1234, today, "note")
        val transfer = transactions.getTransfer(id)
        assertNotNull(transfer)
        assertEquals(a, transfer.fromAccountId)
        assertEquals(b, transfer.toAccountId)

        transactions.updateTransfer(id, b, a, 999, today, null)
        val updated = transactions.getTransfer(id)!!
        assertEquals(b, updated.fromAccountId)
        assertEquals(999, updated.amount)

        transactions.deleteTransfer(id)
        assertTrue(transactions.observeFiltered(TransactionFilter()).first().isEmpty())
    }

    @Test
    fun transfersAppearOnceInCrossAccountListsAndOncePerAccount() = runTest {
        val a = accounts.add("A", AccountType.CASH, 0, today)
        val b = accounts.add("B", AccountType.BANK, 0, today)
        transactions.addTransfer(a, b, 1000, today, null)
        transactions.add(a, null, TransactionType.EXPENSE, 50, today, null)

        val all = transactions.observeFiltered(TransactionFilter()).first()
        assertEquals(2, all.size)
        assertEquals(TransactionType.TRANSFER_OUT, all.first { it.type.isTransfer }.type)
        assertEquals(TransactionType.TRANSFER_OUT, transactions.observeFiltered(TransactionFilter(accountId = a)).first().first { it.type.isTransfer }.type)
        assertEquals(TransactionType.TRANSFER_IN, transactions.observeFiltered(TransactionFilter(accountId = b)).first().single().type)
        assertEquals(2, transactions.observeRecent(10).first().size)
        // The account view still shows both legs from each side's perspective.
        assertEquals(1, transactions.observeByAccount(b).first().size)
    }

    @Test
    fun deletingAccountPausesOrRemovesItsRules() = runTest {
        val cash = accounts.add("Cash", AccountType.CASH, 0, today)
        val bank = accounts.add("Bank", AccountType.BANK, 0, today)
        transactions.add(cash, null, TransactionType.EXPENSE, 100, today, null)
        rules.add(500, cash, null, TransactionType.EXPENSE, Frequency.MONTHLY, today, "cash rule")
        rules.add(500, bank, null, TransactionType.EXPENSE, Frequency.MONTHLY, today, "bank rule")

        val useCase = DeleteAccountUseCase(accounts, transactions, rules)
        assertEquals(DeleteAccountUseCase.Outcome.ARCHIVED, useCase(cash))
        assertEquals(DeleteAccountUseCase.Outcome.DELETED, useCase(bank))
        val remaining = rules.observeAll().first()
        assertEquals(listOf("cash rule"), remaining.map { it.note })
        assertTrue(remaining.none { it.isActive })
        assertEquals(null, rules.nextActiveDueDate())
    }

    @Test
    fun filteredQueryAndSearch() = runTest {
        val a = accounts.add("A", AccountType.CASH, 0, today)
        val b = accounts.add("B", AccountType.BANK, 0, today)
        val food = categories.add("Food", "🍔", "#000000", CategoryType.EXPENSE, false, false)
        transactions.add(a, food, TransactionType.EXPENSE, 12050, LocalDate(2026, 9, 1), "lunch")
        transactions.add(b, food, TransactionType.EXPENSE, 500, LocalDate(2026, 8, 1), "coffee")

        assertEquals(1, transactions.observeFiltered(TransactionFilter(accountId = a)).first().size)
        assertEquals(2, transactions.observeFiltered(TransactionFilter(categoryId = food)).first().size)
        assertEquals(1, transactions.observeFiltered(TransactionFilter(fromDate = LocalDate(2026, 9, 1))).first().size)
        assertEquals("lunch", transactions.observeFiltered(TransactionFilter(query = "LUN")).first().single().note)
        assertEquals("lunch", transactions.observeFiltered(TransactionFilter(query = "120.50")).first().single().note)
    }

    @Test
    fun deletingCategoryReassignsToUncategorized() = runTest {
        DatabaseSeeder(accounts, categories, FixedDate(today)).seedIfEmpty()
        val cash = accounts.observeAll().first().single().id
        val food = categories.getAll().first { it.name == "Food" }
        val fallback = categories.getDefault(CategoryType.EXPENSE)!!
        transactions.add(cash, food.id, TransactionType.EXPENSE, 100, today, null)
        rules.add(100, cash, food.id, TransactionType.EXPENSE, Frequency.MONTHLY, today, null)
        budgets.set(food.id, 5000)

        val result = DeleteCategoryUseCase(categories, transactions, budgets, rules)(food.id)
        assertIs<Result.Success<Unit>>(result)
        assertEquals(fallback.id, transactions.observeFiltered(TransactionFilter()).first().single().categoryId)
        assertEquals(fallback.id, rules.observeAll().first().single().categoryId)
        assertTrue(budgets.observeAll().first().isEmpty())
        assertEquals(null, categories.getById(food.id))
        assertIs<Result.Failure>(DeleteCategoryUseCase(categories, transactions, budgets, rules)(fallback.id))
    }
}

class InsightsQueriesTest {
    private val today = LocalDate(2026, 9, 5)

    @Test
    fun categoryAndMonthlyTotalsFromSql() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        FinanceDatabase.Schema.create(driver)
        val db = FinanceDatabase(driver)
        val exec = DbExecutor(db)
        val accounts = AccountRepositoryImpl(db, exec)
        val categories = CategoryRepositoryImpl(db, exec)
        val transactions = TransactionRepositoryImpl(db, exec)
        val budgets = BudgetRepositoryImpl(db, exec)

        val cash = accounts.add("Cash", AccountType.CASH, 0, today)
        val food = categories.add("Food", "f", "#000", CategoryType.EXPENSE, false, false)
        val rent = categories.add("Rent", "r", "#000", CategoryType.EXPENSE, false, false)
        transactions.add(cash, food, TransactionType.EXPENSE, 300, LocalDate(2026, 9, 2), null)
        transactions.add(cash, food, TransactionType.EXPENSE, 200, LocalDate(2026, 9, 3), null)
        transactions.add(cash, rent, TransactionType.EXPENSE, 900, LocalDate(2026, 9, 1), null)
        transactions.add(cash, null, TransactionType.INCOME, 5000, LocalDate(2026, 8, 31), null)
        transactions.addTransfer(cash, cash, 10, LocalDate(2026, 9, 4), null)

        val byCat = transactions.observeCategoryTotals(LocalDate(2026, 9, 1), LocalDate(2026, 9, 30), TransactionType.EXPENSE).first()
        assertEquals(listOf(rent to 900L, food to 500L), byCat.map { it.categoryId to it.total })

        val months = transactions.observeMonthlyTotals(LocalDate(2026, 8, 1), LocalDate(2026, 9, 30)).first()
        assertEquals(2, months.size)
        assertEquals(LocalDate(2026, 8, 1), months[0].month)
        assertEquals(5000, months[0].income)
        assertEquals(1400, months[1].expense)
        assertEquals(0, months[1].income) // transfers never count

        budgets.set(food, 1000)
        budgets.set(food, 400) // upsert
        assertEquals(listOf(400L), budgets.observeAll().first().map { it.monthlyLimit })
    }
}
