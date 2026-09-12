package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.Money
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionFilter
import com.codigitech.ft.domain.model.TransactionKind
import com.codigitech.ft.domain.model.TransactionType
import com.codigitech.ft.domain.repository.AccountRepository
import com.codigitech.ft.domain.repository.CategoryRepository
import com.codigitech.ft.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.first

/**
 * Everything the user has recorded as one RFC 4180 CSV, newest first. Transfers appear once, with
 * both accounts. Amounts are plain decimals (no symbol, no grouping) so spreadsheets parse them.
 */
class ExportTransactionsCsvUseCase(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val categories: CategoryRepository,
) {
    data class Export(val fileName: String, val csv: String, val rowCount: Int)

    suspend operator fun invoke(): Export {
        val rows = transactions.observeFiltered(TransactionFilter()).first()
        val accountsById = accounts.observeAll().first().associateBy { it.id }
        val categoriesById = categories.getAll().associateBy { it.id }
        val sb = StringBuilder()
        sb.appendLine(HEADER.joinToString(","))
        rows.forEach { t -> sb.appendLine(t.toCsvRow(accountsById.mapValues { it.value.name }, categoriesById.mapValues { it.value.name })) }
        val newest = rows.firstOrNull()?.date
        val fileName = "pocketsum-transactions" + (newest?.let { "-$it" } ?: "") + ".csv"
        return Export(fileName, sb.toString(), rows.size)
    }

    private fun Transaction.toCsvRow(accountNames: Map<Long, String>, categoryNames: Map<Long, String>): String {
        val kind = TransactionKind.of(type)
        val signed = when (type) {
            TransactionType.INCOME -> amount
            TransactionType.EXPENSE -> -amount
            else -> amount // transfers are neither income nor expense; reported unsigned
        }
        val to = if (type.isTransfer) counterpartAccountId?.let(accountNames::get).orEmpty() else ""
        return listOf(
            date.toString(),
            kind.label,
            Money.toInputString(signed.let { if (it < 0) -it else it }).let { if (signed < 0) "-$it" else it },
            accountNames[accountId].orEmpty(),
            to,
            if (type.isTransfer) "" else categoryId?.let(categoryNames::get) ?: "Uncategorized",
            note.orEmpty(),
            if (recurringRuleId != null) "yes" else "no",
        ).joinToString(",") { it.csvEscape() }
    }

    private fun String.csvEscape(): String =
        if (any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + replace("\"", "\"\"") + "\"" else this

    companion object {
        val HEADER = listOf("Date", "Type", "Amount", "Account", "To account", "Category", "Note", "Recurring")
    }
}
