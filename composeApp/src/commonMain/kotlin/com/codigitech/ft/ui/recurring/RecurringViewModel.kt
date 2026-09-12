package com.codigitech.ft.ui.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.usecase.AddRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.DeleteRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.Result
import com.codigitech.ft.domain.usecase.ToggleRecurringRuleUseCase
import com.codigitech.ft.domain.usecase.UpdateRecurringRuleUseCase
import com.codigitech.ft.platform.RecurringSyncCoordinator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class RecurringState(
    val rules: List<RecurringRule> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accountsById: Map<Long, Account> = emptyMap(),
    val categoriesById: Map<Long, Category> = emptyMap(),
    val loaded: Boolean = false,
)

class RecurringViewModel(
    rules: RecurringRuleRepository,
    accounts: AccountRepository,
    categories: CategoryRepository,
    private val addRule: AddRecurringRuleUseCase,
    private val updateRule: UpdateRecurringRuleUseCase,
    private val toggleRule: ToggleRecurringRuleUseCase,
    private val deleteRule: DeleteRecurringRuleUseCase,
    private val sync: RecurringSyncCoordinator,
) : ViewModel() {
    val state: StateFlow<RecurringState> = combine(
        rules.observeAll(), accounts.observeActive(), categories.observeAll(),
    ) { r, a, c ->
        RecurringState(r, a, c, a.associateBy { it.id }, c.associateBy { it.id }, loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecurringState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun save(
        id: Long?, amount: Long, accountId: Long, categoryId: Long?, type: TransactionType,
        frequency: Frequency, startDate: LocalDate, note: String?,
    ) {
        viewModelScope.launch {
            val result = if (id == null) addRule(amount, accountId, categoryId, type, frequency, startDate, note)
            else updateRule(id, amount, accountId, categoryId, type, frequency, startDate, note)
            if (result is Result.Failure) {
                _messages.tryEmit(result.message)
            } else {
                // A rule starting today or earlier generates immediately.
                val generated = sync.sync(notify = false)
                if (generated > 0) _messages.tryEmit("$generated transaction${if (generated == 1) "" else "s"} generated")
            }
        }
    }

    fun toggle(id: Long, active: Boolean) {
        viewModelScope.launch {
            toggleRule(id, active)
            sync.rescheduleReminder()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            deleteRule(id)
            sync.rescheduleReminder()
        }
    }
}
