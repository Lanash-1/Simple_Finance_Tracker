package com.codigitech.ft.data.db

import com.codigitech.ft.db.Account as AccountRow
import com.codigitech.ft.db.Category as CategoryRow
import com.codigitech.ft.db.RecurringRule as RecurringRuleRow
import com.codigitech.ft.db.TransactionEntity as TransactionRow
import com.codigitech.ft.domain.model.Account
import com.codigitech.ft.domain.model.AccountType
import com.codigitech.ft.domain.model.Category
import com.codigitech.ft.domain.model.CategoryType
import com.codigitech.ft.domain.model.Frequency
import com.codigitech.ft.domain.model.RecurringRule
import com.codigitech.ft.domain.model.Transaction
import com.codigitech.ft.domain.model.TransactionType
import kotlinx.datetime.LocalDate

fun LocalDate.toEpochDay(): Long = toEpochDays()
fun Long.toLocalDate(): LocalDate = LocalDate.fromEpochDays(this)
fun Boolean.toDb(): Long = if (this) 1L else 0L
fun Long.toBool(): Boolean = this != 0L

fun AccountRow.toDomain() = Account(
    id = id,
    name = name,
    type = AccountType.fromName(type),
    initialBalance = initialBalance,
    createdAt = createdAt.toLocalDate(),
    isArchived = isArchived.toBool(),
)

fun CategoryRow.toDomain() = Category(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    type = CategoryType.fromName(type),
    isCustom = isCustom.toBool(),
    isDefault = isDefault.toBool(),
)

fun TransactionRow.toDomain() = Transaction(
    id = id,
    accountId = accountId,
    categoryId = categoryId,
    type = TransactionType.fromName(type),
    amount = amount,
    date = date.toLocalDate(),
    note = note,
    transferId = transferId,
    counterpartAccountId = counterpartAccountId,
    recurringRuleId = recurringRuleId,
)

fun RecurringRuleRow.toDomain() = RecurringRule(
    id = id,
    amount = amount,
    accountId = accountId,
    categoryId = categoryId,
    type = TransactionType.fromName(type),
    frequency = Frequency.fromName(frequency),
    startDate = startDate.toLocalDate(),
    nextDueDate = nextDueDate.toLocalDate(),
    isActive = isActive.toBool(),
    note = note,
)
