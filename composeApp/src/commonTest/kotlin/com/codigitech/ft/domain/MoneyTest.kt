package com.codigitech.ft.domain

import com.codigitech.ft.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    @Test
    fun formatsWithIndianGrouping() {
        assertEquals("₹0.00", Money.format(0))
        assertEquals("₹5.00", Money.format(500))
        assertEquals("₹1,234.50", Money.format(123450))
        assertEquals("₹12,34,567.89", Money.format(123456789))
        assertEquals("-₹2,500.00", Money.format(-250000))
        assertEquals("+₹10.00", Money.format(1000, withSign = true))
    }

    @Test
    fun parsesUserInput() {
        assertEquals(123450, Money.parse("1234.5"))
        assertEquals(123450, Money.parse("1,234.50"))
        assertEquals(100000, Money.parse(" 1000 "))
        assertEquals(5, Money.parse("0.05"))
        assertNull(Money.parse(""))
        assertNull(Money.parse("0"))
        assertNull(Money.parse("-5"))
        assertNull(Money.parse("1.234"))
        assertNull(Money.parse("abc"))
    }

    @Test
    fun roundTripsThroughInputString() {
        for (v in listOf(1L, 100L, 12345L, 999999999L)) {
            assertEquals(v, Money.parse(Money.toInputString(v)))
        }
    }
}
