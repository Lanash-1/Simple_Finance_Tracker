package com.codigitech.ft.domain.model

import kotlinx.datetime.LocalDate

enum class AccountType(val label: String) {
    CASH("Cash"), BANK("Bank"), CARD("Card"), OTHER("Other");

    companion object {
        fun fromName(name: String): AccountType = entries.firstOrNull { it.name == name } ?: OTHER
    }
}

data class Account(
    val id: Long,
    val name: String,
    val type: AccountType,
    val initialBalance: Long,
    val createdAt: LocalDate,
    val isArchived: Boolean,
)

data class AccountWithBalance(
    val account: Account,
    val balance: Long,
)
