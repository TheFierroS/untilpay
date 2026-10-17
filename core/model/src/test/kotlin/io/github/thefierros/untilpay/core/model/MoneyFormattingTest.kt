package io.github.thefierros.untilpay.core.model

import java.util.Currency
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyFormattingTest {

    private val lira = Currency.getInstance("TRY")
    private val turkish = Locale.forLanguageTag("tr-TR")

    /**
     * Locale data uses non-breaking spaces between the symbol and the number
     * in some languages; normalizing them keeps the expectations readable.
     */
    private fun Money.formatted(locale: Locale) =
        format(locale).replace('\u00A0', ' ').replace('\u202F', ' ')

    @Test
    fun `turkish lira in Turkish`() {
        assertEquals("₺1.500,50", Money(150050, lira).formatted(turkish))
    }

    @Test
    fun `negative turkish lira in Turkish`() {
        assertEquals("-₺1.500,50", Money(-150050, lira).formatted(turkish))
    }

    @Test
    fun `whole amounts still show the decimal places of the currency`() {
        assertEquals("₺15,00", Money(1500, lira).formatted(turkish))
    }

    @Test
    fun `euro in German puts the symbol after the amount`() {
        val euro = Currency.getInstance("EUR")
        assertEquals("1.500,50 €", Money(150050, euro).formatted(Locale.GERMANY))
    }

    @Test
    fun `us dollar in American English`() {
        val dollar = Currency.getInstance("USD")
        assertEquals("\$1,500.50", Money(150050, dollar).formatted(Locale.US))
    }

    @Test
    fun `currencies without minor units show no decimals`() {
        val yen = Currency.getInstance("JPY")
        assertEquals("¥1,500", Money(1500, yen).formatted(Locale.US))
    }
}
