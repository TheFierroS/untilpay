package io.github.thefierros.untilpay.core.model

import java.math.BigDecimal
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

    @Test
    fun `of converts a decimal amount into minor units`() {
        assertEquals(Money(1550, lira), Money.of(BigDecimal("15.50"), lira))
        assertEquals(Money(1550, lira), Money.of(BigDecimal("15.5"), lira))
        assertEquals(Money(150000, lira), Money.of(BigDecimal("1500"), lira))
        assertEquals(Money(-1550, lira), Money.of(BigDecimal("-15.50"), lira))
    }

    @Test
    fun `of follows the minor units of each currency`() {
        val yen = Currency.getInstance("JPY")
        val dinar = Currency.getInstance("KWD")
        assertEquals(Money(1500, yen), Money.of(BigDecimal("1500"), yen))
        assertEquals(Money(2125, dinar), Money.of(BigDecimal("2.125"), dinar))
    }

    @Test
    fun `of accepts trailing zeros beyond the minor units`() {
        assertEquals(Money(1550, lira), Money.of(BigDecimal("15.500"), lira))
    }

    @Test
    fun `of rejects more decimal places than the currency allows`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of(BigDecimal("15.505"), lira)
        }
        assertThrows(IllegalArgumentException::class.java) {
            Money.of(BigDecimal("1500.5"), Currency.getInstance("JPY"))
        }
    }

    @Test
    fun `of rejects amounts too large to represent`() {
        assertThrows(ArithmeticException::class.java) {
            Money.of(BigDecimal("1E+30"), lira)
        }
    }

    @Test
    fun `toBigDecimal returns the amount in major units`() {
        assertEquals(BigDecimal("15.50"), Money(1550, lira).toBigDecimal())
        assertEquals(BigDecimal("-0.05"), Money(-5, lira).toBigDecimal())
        assertEquals(BigDecimal("1500"), Money(1500, Currency.getInstance("JPY")).toBigDecimal())
    }
}
