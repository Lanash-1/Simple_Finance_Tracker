package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.model.Transfer
import com.codigitech.ft.domain.repository.TransactionRepository
import kotlinx.datetime.LocalDate

/** Adds an income or expense. Validates amount and type. */
class AddTransactionUseCase(private val transactions: TransactionRepository) {
    suspend operator fun invoke(
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        note: String?,
    ): Result<Long> {
        if (type.isTransfer) return Result.Failure("Use the transfer use case for transfers")
        if (amount <= 0) return Result.Failure("Amount must be greater than zero")
        val id = transactions.add(accountId, categoryId, type, amount, date, note?.trim()?.ifBlank { null })
        return Result.Success(id)
    }
}

class UpdateTransactionUseCase(private val transactions: TransactionRepository) {
    suspend operator fun invoke(
        id: Long,
        accountId: Long,
        categoryId: Long?,
        type: TransactionType,
        amount: Long,
        date: LocalDate,
        note: String?,
    ): Result<Unit> {
        if (type.isTransfer) return Result.Failure("Use the transfer use case for transfers")
        if (amount <= 0) return Result.Failure("Amount must be greater than zero")
        transactions.update(id, accountId, categoryId, type, amount, date, note?.trim()?.ifBlank { null })
        return Result.Success(Unit)
    }
}

/**
 * Everything needed to put a deleted entry back. Restoring inserts new rows (new ids) with the
 * same content, which is what "Undo" in the UI relies on.
 */
sealed interface DeletedEntry {
    data class Single(val transaction: Transaction) : DeletedEntry
    data class Pair(val transfer: Transfer) : DeletedEntry
}

class DeleteTransactionUseCase(private val transactions: TransactionRepository) {
    /** Deletes the entry (both legs for a transfer) and returns a snapshot for undo, or null if it no longer exists. */
    suspend operator fun invoke(id: Long): DeletedEntry? {
        val existing = transactions.getById(id) ?: return null
        val transferId = existing.transferId
        return if (transferId != null) {
            val transfer = transactions.getTransfer(transferId)
            transactions.deleteTransfer(transferId)
            transfer?.let { DeletedEntry.Pair(it) }
        } else {
            transactions.delete(id)
            DeletedEntry.Single(existing)
        }
    }
}

class RestoreTransactionUseCase(private val transactions: TransactionRepository) {
    suspend operator fun invoke(entry: DeletedEntry) {
        when (entry) {
            is DeletedEntry.Single -> with(entry.transaction) {
                transactions.add(accountId, categoryId, type, amount, date, note, recurringRuleId)
            }
            is DeletedEntry.Pair -> with(entry.transfer) {
                transactions.addTransfer(fromAccountId, toAccountId, amount, date, note)
            }
        }
    }
}

/** Moves money between two owned accounts as a linked pair of rows. */
class TransferUseCase(private val transactions: TransactionRepository) {
    suspend fun add(fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?): Result<String> {
        validate(fromAccountId, toAccountId, amount)?.let { return it }
        return Result.Success(transactions.addTransfer(fromAccountId, toAccountId, amount, date, note?.trim()?.ifBlank { null }))
    }

    suspend fun update(transferId: String, fromAccountId: Long, toAccountId: Long, amount: Long, date: LocalDate, note: String?): Result<Unit> {
        validate(fromAccountId, toAccountId, amount)?.let { return it }
        transactions.updateTransfer(transferId, fromAccountId, toAccountId, amount, date, note?.trim()?.ifBlank { null })
        return Result.Success(Unit)
    }

    private fun validate(from: Long, to: Long, amount: Long): Result.Failure? = when {
        from == to -> Result.Failure("Cannot transfer to the same account")
        amount <= 0 -> Result.Failure("Amount must be greater than zero")
        else -> null
    }
}
