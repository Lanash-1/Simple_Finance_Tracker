package com.codigitech.ft.domain.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus

enum class Frequency(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly");

    fun next(from: LocalDate): LocalDate = when (this) {
        DAILY -> from.plus(1, DateTimeUnit.DAY)
        WEEKLY -> from.plus(1, DateTimeUnit.WEEK)
        MONTHLY -> from.plus(1, DateTimeUnit.MONTH)
    }

    /**
     * Next occurrence after [from] for a schedule that started on [anchor]. Monthly rules keep the
     * anchor's day of month, so a rule that started on the 31st lands on Feb 28 and then Mar 31
     * instead of drifting to the 28th for good. Daily and weekly schedules never drift.
     */
    fun next(from: LocalDate, anchor: LocalDate): LocalDate {
        if (this != MONTHLY) return next(from)
        val monthsElapsed = (from.year - anchor.year) * 12 + (from.month.number - anchor.month.number)
        var candidate = anchor.plus((monthsElapsed + 1).toLong(), DateTimeUnit.MONTH)
        // `from` can be later in the month than the anchor's clamped day (e.g. anchor 31st, from Mar 30).
        while (candidate <= from) candidate = anchor.plus((monthsElapsed + 2).toLong(), DateTimeUnit.MONTH)
        return candidate
    }

    companion object {
        fun fromName(name: String): Frequency = entries.firstOrNull { it.name == name } ?: MONTHLY
    }
}

data class RecurringRule(
    val id: Long,
    val amount: Long,
    val accountId: Long,
    val categoryId: Long?,
    val type: TransactionType,
    val frequency: Frequency,
    val startDate: LocalDate,
    val nextDueDate: LocalDate,
    val isActive: Boolean,
    val note: String?,
)
