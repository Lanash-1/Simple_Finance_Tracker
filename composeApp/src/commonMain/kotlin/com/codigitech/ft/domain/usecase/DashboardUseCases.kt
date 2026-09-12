package com.codigitech.ft.domain.usecase

import com.codigitech.ft.domain.model.AccountWithBalance
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Combined balance across active accounts only. */
class CalculateBalanceUseCase {
    operator fun invoke(balances: List<AccountWithBalance>): Long =
        balances.filter { !it.account.isArchived }.sumOf { it.balance }
}

object MonthRange {
    fun of(date: LocalDate): Pair<LocalDate, LocalDate> {
        val first = LocalDate(date.year, date.month, 1)
        val last = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return first to last
    }
}
