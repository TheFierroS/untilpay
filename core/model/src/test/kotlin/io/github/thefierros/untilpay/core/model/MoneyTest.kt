package io.github.thefierros.untilpay.core.model

import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")

    @Test
    fun `amounts in the same currency can be added and subtracted`() {
        assertEquals(Money(1550, lira), Money(1000, lira) + Money(550, lira))
        assertEquals(Money(450, lira), Money(1000, lira) - Money(550, lira))
    }

    @Test
    fun `division rounds down to the nearest minor unit`() {
        assertEquals(Money(333, lira), Money(1000, lira) / 3L)
    }

    @Test
    fun `division of a negative amount rounds toward negative infinity`() {
        assertEquals(Money(-334, lira), Money(-1000, lira) / 3L)
    }

    @Test
    fun `division by zero is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(1000, lira) / 0L
        }
    }

    @Test
    fun `division by a negative number is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(1000, lira) / -2L
        }
    }

    @Test
    fun `adding different currencies is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100, lira) + Money(100, euro)
        }
    }

    @Test
    fun `comparing different currencies is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money(100, lira) < Money(100, euro)
        }
    }

    @Test
    fun `currencies without fixed minor units are rejected`() {
        // XAU (gold) has an ISO 4217 code but no fixed number of minor units.
        val gold = Currency.getInstance("XAU")
        assertThrows(IllegalArgumentException::class.java) {
            Money(1, gold)
        }
    }

    @Test
    fun `overflow is detected instead of wrapping around`() {
        assertThrows(ArithmeticException::class.java) {
            Money(Long.MAX_VALUE, lira) + Money(1, lira)
        }
    }

    @Test
    fun `zero is neither positive nor negative`() {
        assertTrue(Money(1, lira).isPositive)
        assertTrue(Money(-1, lira).isNegative)
        assertFalse(Money.zero(lira).isPositive)
        assertFalse(Money.zero(lira).isNegative)
    }
}
