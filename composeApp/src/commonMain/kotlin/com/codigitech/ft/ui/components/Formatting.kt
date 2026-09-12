package com.codigitech.ft.ui.components

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.minus

private fun Month.short(): String = name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
private fun Month.long(): String = name.lowercase().replaceFirstChar { it.uppercase() }
private fun DayOfWeek.short(): String = name.lowercase().replaceFirstChar { it.uppercase() }.take(3)

/** 3 Sep 2026 */
fun LocalDate.display(): String = "$day ${month.short()} $year"

/** September 2026 */
fun LocalDate.monthTitle(): String = "${month.long()} $year"

/** Sep */
fun LocalDate.monthShort(): String = month.short()

/** Sep '26 */
fun LocalDate.monthShortWithYear(): String = "${month.short()} '${(year % 100).toString().padStart(2, '0')}"

/** Today / Yesterday / Wed, 3 Sep / Wed, 3 Sep 2025 */
fun LocalDate.relativeLabel(today: LocalDate): String = when (this) {
    today -> "Today"
    today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
    else -> buildString {
        append(dayOfWeek.short()).append(", ").append(day).append(' ').append(month.short())
        if (year != today.year) append(' ').append(year)
    }
}

fun Int.pluralize(singular: String, plural: String = singular + "s") = "$this ${if (this == 1) singular else plural}"
