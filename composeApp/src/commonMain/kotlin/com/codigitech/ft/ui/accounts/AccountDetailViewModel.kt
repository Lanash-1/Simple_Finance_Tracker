package com.codigitech.ft.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.domain.usecase.DeleteAccountUseCase
import com.codigitech.ft.domain.usecase.DeleteTransactionUseCase
import com.codigitech.ft.domain.usecase.DeletedEntry
import com.codigitech.ft.domain.usecase.MonthRange
import com.codigitech.ft.domain.usecase.RestoreTransactionUseCase
import com.codigitech.ft.ui.components.DaySection
import com.codigitech.ft.ui.components.groupByDay
import com.codigitech.ft.ui.transactions.ListEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class AccountDetailState(
    val account: Account? = null,
    val balance: Long = 0,
    val monthIn: Long = 0,
    val monthOut: Long = 0,
    val sections: List<DaySection> = emptyList(),
    val transactionCount: Int = 0,
    val accountsById: Map<Long, Account> = emptyMap(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val today: LocalDate? = null,
    val loaded: Boolean = false,
)

class AccountDetailViewModel(
    private val accountId: Long,
    accounts: AccountRepository,
    transactions: TransactionRepository,
    categories: CategoryRepository,
    private val deleteAccount: DeleteAccountUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val restoreTransaction: RestoreTransactionUseCase,
    dates: DateProvider,
) : ViewModel() {
    private val today = dates.today()
    private val monthRange = MonthRange.of(today)

    private val _events = MutableSharedFlow<ListEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ListEvent> = _events
    private var lastDeleted: DeletedEntry? = null

    val state: StateFlow<AccountDetailState> = combine(
        accounts.observeBalances(),
        transactions.observeByAccount(accountId),
        categories.observeAll(),
    ) { balances, txns, cats ->
        val mine = balances.firstOrNull { it.account.id == accountId }
        val thisMonth = txns.filter { it.date >= monthRange.first && it.date <= monthRange.second }
        AccountDetailState(
            account = mine?.account,
            balance = mine?.balance ?: 0,
            monthIn = thisMonth.filter { it.type.sign > 0 }.sumOf { it.amount },
            monthOut = thisMonth.filter { it.type.sign < 0 }.sumOf { it.amount },
            sections = txns.groupByDay(),
            transactionCount = txns.size,
            accountsById = balances.associate { it.account.id to it.account },
            categoriesById = cats.associateBy { it.id },
            today = today,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountDetailState())

    fun delete(onDone: (DeleteAccountUseCase.Outcome) -> Unit) {
        viewModelScope.launch { onDone(deleteAccount(accountId)) }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            val snapshot = deleteTransaction(transaction.id) ?: return@launch
            lastDeleted = snapshot
            _events.tryEmit(ListEvent.Deleted(if (transaction.type.isTransfer) "Transfer deleted" else "Transaction deleted"))
        }
    }

    fun undoDelete() {
        val entry = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { restoreTransaction(entry); _events.tryEmit(ListEvent.Restored) }
    }
}
