package com.codigitech.ft.domain.model

import kotlinx.datetime.LocalDate

/** Total of one category over a period. `categoryId` is null for rows without a category. */
data class CategoryTotal(val categoryId: Long?, val total: Long)

/** Income and expense for one calendar month; [month] is the first day of that month. */
data class MonthTotals(val month: LocalDate, val income: Long, val expense: Long) {
    val net: Long get() = income - expense
}

/** Monthly spending cap for one expense category. */
data class Budget(val categoryId: Long, val monthlyLimit: Long)

data class BudgetProgress(val category: Category, val limit: Long, val spent: Long) {
    val fraction: Float get() = if (limit <= 0) 0f else spent.toFloat() / limit.toFloat()
    val remaining: Long get() = limit - spent
    val isOver: Boolean get() = spent > limit
}
