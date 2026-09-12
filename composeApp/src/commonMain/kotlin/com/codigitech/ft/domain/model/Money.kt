package com.codigitech.ft.domain.model

import kotlin.math.abs

/** Amounts are stored as Long minor units (paise). Single currency: INR. */
object Money {
    const val SYMBOL = "₹"
    private const val MINOR_PER_MAJOR = 100L

    fun format(minor: Long, withSign: Boolean = false): String {
        val negative = minor < 0
        val absValue = abs(minor)
        val major = absValue / MINOR_PER_MAJOR
        val fraction = absValue % MINOR_PER_MAJOR
        val body = "$SYMBOL${groupIndian(major)}.${fraction.toString().padStart(2, '0')}"
        return when {
            negative -> "-$body"
            withSign -> "+$body"
            else -> body
        }
    }

    /** Indian digit grouping: 12,34,567 */
    private fun groupIndian(value: Long): String {
        val s = value.toString()
        if (s.length <= 3) return s
        val last3 = s.takeLast(3)
        val rest = s.dropLast(3)
        val grouped = rest.reversed().chunked(2).joinToString(",").reversed()
        return "$grouped,$last3"
    }

    /** Parses user input like "1234.5" or "1,234" to minor units. Returns null when invalid or non-positive. */
    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(",", "").replace(SYMBOL, "")
        if (cleaned.isEmpty()) return null
        val regex = Regex("^\\d{1,13}(\\.\\d{0,2})?$")
        if (!regex.matches(cleaned)) return null
        val parts = cleaned.split(".")
        val major = parts[0].toLong()
        val fractionStr = parts.getOrNull(1).orEmpty().padEnd(2, '0')
        val fraction = fractionStr.toLong()
        val result = major * MINOR_PER_MAJOR + fraction
        return if (result > 0) result else null
    }

    fun toInputString(minor: Long): String {
        val major = minor / MINOR_PER_MAJOR
        val fraction = minor % MINOR_PER_MAJOR
        return if (fraction == 0L) major.toString() else "$major.${fraction.toString().padStart(2, '0')}"
    }
}
