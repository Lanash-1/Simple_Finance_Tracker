package com.codigitech.ft.domain.model

import kotlinx.datetime.LocalDate

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER_OUT, TRANSFER_IN;

    val isTransfer: Boolean get() = this == TRANSFER_OUT || this == TRANSFER_IN

    /** +1 when the movement increases the account balance, -1 when it decreases it. */
    val sign: Int get() = if (this == INCOME || this == TRANSFER_IN) 1 else -1

    companion object {
        fun fromName(name: String): TransactionType = entries.firstOrNull { it.name == name } ?: EXPENSE
    }
}

/** The three kinds a user thinks in; the two transfer legs collapse into one. */
enum class TransactionKind(val label: String) {
    EXPENSE("Expense"), INCOME("Income"), TRANSFER("Transfer");

    companion object {
        fun of(type: TransactionType): TransactionKind = when (type) {
            TransactionType.INCOME -> INCOME
            TransactionType.EXPENSE -> EXPENSE
            TransactionType.TRANSFER_OUT, TransactionType.TRANSFER_IN -> TRANSFER
        }
    }
}

data class Transaction(
    val id: Long,
    val accountId: Long,
    val categoryId: Long?,
    val type: TransactionType,
    val amount: Long,
    val date: LocalDate,
    val note: String?,
    val transferId: String?,
    val counterpartAccountId: Long?,
    val recurringRuleId: Long?,
) {
    /** Signed effect on the owning account's balance. */
    val signedAmount: Long get() = amount * type.sign
    val kind: TransactionKind get() = TransactionKind.of(type)
}

/** A transfer viewed as one logical operation (built from its two legs). */
data class Transfer(
    val transferId: String,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Long,
    val date: LocalDate,
    val note: String?,
)

data class TransactionFilter(
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val query: String = "",
    val kind: TransactionKind? = null,
) {
    val isEmpty: Boolean
        get() = accountId == null && categoryId == null && fromDate == null && toDate == null && query.isBlank() && kind == null

    /** Filters other than the free-text query and the kind chips, i.e. what lives in the filter sheet. */
    val hasAdvanced: Boolean get() = accountId != null || categoryId != null || fromDate != null || toDate != null
}

data class PeriodTotals(val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}
