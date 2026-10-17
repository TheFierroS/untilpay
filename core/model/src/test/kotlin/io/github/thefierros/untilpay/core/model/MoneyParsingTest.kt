package io.github.thefierros.untilpay.core.model

import java.util.Currency
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyParsingTest {

    private val lira = Currency.getInstance("TRY")
    private val turkish = Locale.forLanguageTag("tr-TR")

    private fun parseLira(text: String) = Money.parseOrNull(text, lira, turkish)

    @Test
    fun `turkish input uses a comma for decimals and a dot for grouping`() {
        assertEquals(Money(150050, lira), parseLira("1.500,50"))
        assertEquals(Money(150050, lira), parseLira("1500,50"))
        assertEquals(Money(1550, lira), parseLira("15,5"))
        assertEquals(Money(1500, lira), parseLira("15"))
    }

    @Test
    fun `surrounding spaces and a leading minus sign are accepted`() {
        assertEquals(Money(1550, lira), parseLira("  15,50 "))
        assertEquals(Money(-1550, lira), parseLira("-15,50"))
    }

    @Test
    fun `partially typed amounts are accepted`() {
        assertEquals(Money(1500, lira), parseLira("15,"))
        assertEquals(Money(50, lira), parseLira(",50"))
    }

    @Test
    fun `a dot without three digits after it is rejected in Turkish`() {
        // Read as grouping, "15.50" would become 1550 lira: a hundred times too much.
        assertNull(parseLira("15.50"))
        assertNull(parseLira("1.50.000"))
    }

    @Test
    fun `invalid input is rejected`() {
        assertNull(parseLira(""))
        assertNull(parseLira("-"))
        assertNull(parseLira("abc"))
        assertNull(parseLira("12abc"))
        assertNull(parseLira("1,2,3"))
        assertNull(parseLira("--5"))
    }

    @Test
    fun `more decimal places than the currency allows are rejected`() {
        assertNull(parseLira("15,505"))
        val yen = Currency.getInstance("JPY")
        assertNull(Money.parseOrNull("1500.5", yen, Locale.US))
    }

    @Test
    fun `amounts too large to represent are rejected`() {
        assertNull(parseLira("99999999999999999999"))
    }

    @Test
    fun `english input uses a dot for decimals and a comma for grouping`() {
        val dollar = Currency.getInstance("USD")
        val yen = Currency.getInstance("JPY")
        assertEquals(Money(150050, dollar), Money.parseOrNull("1,500.50", dollar, Locale.US))
        assertEquals(Money(1500, yen), Money.parseOrNull("1,500", yen, Locale.US))
    }

    @Test
    fun `a plain space groups thousands where the locale uses a special space`() {
        val euro = Currency.getInstance("EUR")
        assertEquals(Money(150050, euro), Money.parseOrNull("1 500,50", euro, Locale.FRANCE))
    }
}
