package com.codigitech.ft.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.AccountWithBalance
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.usecase.AddAccountUseCase
import com.codigitech.ft.domain.usecase.DeleteAccountUseCase
import com.codigitech.ft.domain.usecase.RestoreAccountUseCase
import com.codigitech.ft.domain.usecase.Result
import com.codigitech.ft.domain.usecase.UpdateAccountUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AccountsState(
    val active: List<AccountWithBalance> = emptyList(),
    val archived: List<AccountWithBalance> = emptyList(),
) {
    val total: Long get() = active.sumOf { it.balance }
}

class AccountsViewModel(
    accounts: AccountRepository,
    private val addAccount: AddAccountUseCase,
    private val updateAccount: UpdateAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val restoreAccount: RestoreAccountUseCase,
) : ViewModel() {
    val state: StateFlow<AccountsState> = accounts.observeBalances()
        .map { list -> AccountsState(active = list.filter { !it.account.isArchived }, archived = list.filter { it.account.isArchived }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    fun save(id: Long?, name: String, type: AccountType, initialBalance: Long) {
        viewModelScope.launch {
            val result = if (id == null) addAccount(name, type, initialBalance) else updateAccount(id, name, type, initialBalance)
            if (result is Result.Failure) _messages.tryEmit(result.message)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            when (deleteAccount(id)) {
                DeleteAccountUseCase.Outcome.ARCHIVED -> _messages.tryEmit("Account archived; its history is kept")
                DeleteAccountUseCase.Outcome.DELETED -> _messages.tryEmit("Account deleted")
            }
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch { restoreAccount(id) }
    }
}
