package com.codigitech.ft.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import com.codigitech.ft.domain.usecase.AddTransactionUseCase
import com.codigitech.ft.domain.usecase.DateProvider
import com.codigitech.ft.domain.usecase.DeleteTransactionUseCase
import com.codigitech.ft.domain.usecase.Result
import com.codigitech.ft.domain.usecase.TransferUseCase
import com.codigitech.ft.domain.usecase.UpdateTransactionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** The three kinds the user picks between in the editor. */
enum class EntryKind(val label: String) { EXPENSE("Expense"), INCOME("Income"), TRANSFER("Transfer") }

data class EditTransactionState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val editingId: Long? = null,
    val editingTransferId: String? = null,
    val kind: EntryKind = EntryKind.EXPENSE,
    val amountText: String = "",
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
    val date: LocalDate? = null,
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val amountError: String? = null,
    val formError: String? = null,
    val saved: Boolean = false,
    val confirmingDelete: Boolean = false,
) {
    /** Transfers cannot become income/expense (and vice versa) once saved. */
    val canSwitchKind: Boolean get() = !isEdit || kind != EntryKind.TRANSFER

    val categoriesForKind: List<Category>
        get() = when (kind) {
            EntryKind.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE }
            EntryKind.INCOME -> categories.filter { it.type == CategoryType.INCOME }
            EntryKind.TRANSFER -> emptyList()
        }
}

class EditTransactionViewModel(
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
    private val transactions: TransactionRepository,
    private val addTransaction: AddTransactionUseCase,
    private val updateTransaction: UpdateTransactionUseCase,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val transfer: TransferUseCase,
    private val dates: DateProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(EditTransactionState())
    val state: StateFlow<EditTransactionState> = _state.asStateFlow()
    private var initialised = false

    fun load(transactionId: Long?, transferId: String?, presetAccountId: Long?) {
        if (initialised) return
        initialised = true
        viewModelScope.launch {
            val activeAccounts = accounts.observeActive().first()
            val allAccounts = accounts.observeAll().first()
            val cats = categories.getAll()
            val today = dates.today()
            when {
                transferId != null -> {
                    val t = transactions.getTransfer(transferId)
                    _state.update {
                        it.copy(
                            loading = false, isEdit = true, editingTransferId = transferId, kind = EntryKind.TRANSFER,
                            amountText = t?.let { tr -> Money.toInputString(tr.amount) } ?: "",
                            accountId = t?.fromAccountId, toAccountId = t?.toAccountId,
                            date = t?.date ?: today, note = t?.note.orEmpty(),
                            accounts = allAccounts, categories = cats,
                        )
                    }
                }
                transactionId != null -> {
                    val t = transactions.getById(transactionId)
                    _state.update {
                        it.copy(
                            loading = false, isEdit = true, editingId = transactionId,
                            kind = if (t?.type == TransactionType.INCOME) EntryKind.INCOME else EntryKind.EXPENSE,
                            amountText = t?.let { tx -> Money.toInputString(tx.amount) } ?: "",
                            accountId = t?.accountId, categoryId = t?.categoryId,
                            date = t?.date ?: today, note = t?.note.orEmpty(),
                            accounts = allAccounts, categories = cats,
                        )
                    }
                }
                else -> {
                    val firstAccount = presetAccountId ?: activeAccounts.firstOrNull()?.id
                    _state.update {
                        it.copy(
                            loading = false, accountId = firstAccount, date = today,
                            accounts = activeAccounts, categories = cats,
                            categoryId = cats.firstOrNull { c -> c.type == CategoryType.EXPENSE && !c.isDefault }?.id,
                        )
                    }
                }
            }
        }
    }

    fun setKind(kind: EntryKind) = _state.update { s ->
        if (s.isEdit && ((s.kind == EntryKind.TRANSFER) != (kind == EntryKind.TRANSFER))) return@update s
        val defaultCat = s.categories.firstOrNull { c -> c.type.name == kind.name && !c.isDefault }?.id
        s.copy(kind = kind, categoryId = if (kind == EntryKind.TRANSFER) null else defaultCat, formError = null)
    }

    fun toggleDeleteConfirm(show: Boolean) = _state.update { it.copy(confirmingDelete = show) }

    fun setAmount(text: String) = _state.update { it.copy(amountText = text, amountError = null) }
    fun setAccount(id: Long) = _state.update { it.copy(accountId = id, formError = null) }
    fun setToAccount(id: Long) = _state.update { it.copy(toAccountId = id, formError = null) }
    fun setCategory(id: Long) = _state.update { it.copy(categoryId = id) }
    fun setDate(date: LocalDate?) = _state.update { it.copy(date = date, formError = null) }
    fun setNote(note: String) = _state.update { it.copy(note = note) }

    fun save() {
        val s = _state.value
        val amount = Money.parse(s.amountText)
        if (amount == null) {
            _state.update { it.copy(amountError = "Enter an amount greater than zero") }
            return
        }
        val date = s.date ?: run { _state.update { it.copy(formError = "Date is required") }; return }
        val accountId = s.accountId ?: run { _state.update { it.copy(formError = "Choose an account") }; return }
        viewModelScope.launch {
            val note = s.note.ifBlank { null }
            val result: Result<*> = when (s.kind) {
                EntryKind.TRANSFER -> {
                    val to = s.toAccountId ?: run { _state.update { it.copy(formError = "Choose a destination account") }; return@launch }
                    if (s.editingTransferId != null) transfer.update(s.editingTransferId, accountId, to, amount, date, note)
                    else transfer.add(accountId, to, amount, date, note)
                }
                else -> {
                    val type = if (s.kind == EntryKind.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
                    val categoryId = s.categoryId ?: s.categoriesForKind.firstOrNull { it.isDefault }?.id
                    if (s.editingId != null) updateTransaction(s.editingId, accountId, categoryId, type, amount, date, note)
                    else addTransaction(accountId, categoryId, type, amount, date, note)
                }
            }
            when (result) {
                is Result.Failure -> _state.update { it.copy(formError = result.message) }
                is Result.Success -> _state.update { it.copy(saved = true) }
            }
        }
    }

    fun delete() {
        val s = _state.value
        viewModelScope.launch {
            when {
                s.editingTransferId != null -> transactions.deleteTransfer(s.editingTransferId)
                s.editingId != null -> deleteTransaction(s.editingId)
            }
            _state.update { it.copy(saved = true) }
        }
    }
}
