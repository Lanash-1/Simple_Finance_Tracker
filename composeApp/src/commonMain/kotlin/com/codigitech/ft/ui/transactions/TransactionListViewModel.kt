package com.codigitech.ft.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionFilter
import com.codigitech.ft.domain.model.TransactionKind
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.domain.usecase.DeleteTransactionUseCase
import com.codigitech.ft.domain.usecase.DeletedEntry
import com.codigitech.ft.domain.usecase.RestoreTransactionUseCase
import com.codigitech.ft.ui.components.DaySection
import com.codigitech.ft.ui.components.groupByDay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class TransactionListState(
    val sections: List<DaySection> = emptyList(),
    val count: Int = 0,
    val income: Long = 0,
    val expense: Long = 0,
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accountsById: Map<Long, Account> = emptyMap(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val today: LocalDate? = null,
    val loaded: Boolean = false,
)

/** One-off signals the screen reacts to (snackbars). */
sealed interface ListEvent {
    data class Deleted(val label: String) : ListEvent
    data object Restored : ListEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionListViewModel(
    accounts: AccountRepository,
    categories: CategoryRepository,
    transactions: TransactionRepository,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val restoreTransaction: RestoreTransactionUseCase,
    dates: DateProvider,
) : ViewModel() {
    private val today = dates.today()
    private val _filter = MutableStateFlow(TransactionFilter())
    val filter: StateFlow<TransactionFilter> = _filter.asStateFlow()

    private val _events = MutableSharedFlow<ListEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ListEvent> = _events

    private var lastDeleted: DeletedEntry? = null

    val state: StateFlow<TransactionListState> = combine(
        _filter.flatMapLatest { transactions.observeFiltered(it) },
        accounts.observeAll(),
        categories.observeAll(),
    ) { items, accs, cats ->
        TransactionListState(
            sections = items.groupByDay(),
            count = items.size,
            income = items.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
            expense = items.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
            accounts = accs,
            categories = cats,
            accountsById = accs.associateBy { it.id },
            categoriesById = cats.associateBy { it.id },
            today = today,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionListState())

    fun presetAccount(accountId: Long?) {
        if (accountId != null && _filter.value.accountId == null) _filter.update { it.copy(accountId = accountId) }
    }

    fun setQuery(q: String) = _filter.update { it.copy(query = q) }
    fun setKind(kind: TransactionKind?) = _filter.update { it.copy(kind = kind) }
    fun setAccount(id: Long?) = _filter.update { it.copy(accountId = id) }
    fun setCategory(id: Long?) = _filter.update { it.copy(categoryId = id) }
    fun setFrom(d: LocalDate?) = _filter.update { it.copy(fromDate = d) }
    fun setTo(d: LocalDate?) = _filter.update { it.copy(toDate = d) }
    fun clearAdvanced() = _filter.update { it.copy(accountId = null, categoryId = null, fromDate = null, toDate = null) }
    fun clear() = _filter.update { TransactionFilter() }

    fun delete(transaction: Transaction) {
        viewModelScope.launch {
            val snapshot = deleteTransaction(transaction.id) ?: return@launch
            lastDeleted = snapshot
            _events.tryEmit(ListEvent.Deleted(if (transaction.type.isTransfer) "Transfer deleted" else "Transaction deleted"))
        }
    }

    fun undoDelete() {
        val entry = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            restoreTransaction(entry)
            _events.tryEmit(ListEvent.Restored)
        }
    }
}
