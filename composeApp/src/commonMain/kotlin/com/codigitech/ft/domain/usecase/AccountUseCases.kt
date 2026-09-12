package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.RecurringRuleRepository
import com.codigitech.ft.domain.repository.TransactionRepository

class AddAccountUseCase(private val accounts: AccountRepository, private val dates: DateProvider) {
    suspend operator fun invoke(name: String, type: AccountType, initialBalance: Long): Result<Long> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Failure("Name is required")
        return Result.Success(accounts.add(trimmed, type, initialBalance, dates.today()))
    }
}

class UpdateAccountUseCase(private val accounts: AccountRepository) {
    suspend operator fun invoke(id: Long, name: String, type: AccountType, initialBalance: Long): Result<Unit> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return Result.Failure("Name is required")
        accounts.update(id, trimmed, type, initialBalance)
        return Result.Success(Unit)
    }
}

/**
 * Deleting an account with history archives it (soft delete). An account with no
 * transactions is removed for real. Either way its recurring rules stop posting:
 * archived -> rules paused (the user can point them elsewhere and resume), deleted -> rules removed.
 */
class DeleteAccountUseCase(
    private val accounts: AccountRepository,
    private val transactions: TransactionRepository,
    private val rules: RecurringRuleRepository,
) {
    enum class Outcome { ARCHIVED, DELETED }

    suspend operator fun invoke(id: Long): Outcome = transactions.inTransaction {
        if (transactions.countByAccount(id) > 0) {
            rules.pauseByAccount(id)
            accounts.setArchived(id, true)
            Outcome.ARCHIVED
        } else {
            rules.deleteByAccount(id)
            accounts.deleteHard(id)
            Outcome.DELETED
        }
    }
}

class RestoreAccountUseCase(private val accounts: AccountRepository) {
    suspend operator fun invoke(id: Long) = accounts.setArchived(id, false)
}
