package io.github.thefierros.untilpay.core.designsystem

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTextTest {
    private val turkish = Locale.forLanguageTag("tr")
    private val english = Locale.forLanguageTag("en")

    @Test
    fun `turkish dotted i becomes dotted capital I`() {
        assertEquals("HARCAYABİLECEĞİN", "harcayabileceğin".toDisplayText(turkish))
    }

    @Test
    fun `turkish dotless i becomes plain capital I`() {
        assertEquals("KALDI", "kaldı".toDisplayText(turkish))
    }

    @Test
    fun `english i becomes plain capital I`() {
        assertEquals("SPENDABLE TODAY", "spendable today".toDisplayText(english))
    }

    @Test
    fun `digits and the lira sign are left as they are`() {
        assertEquals("42,00 ₺", "42,00 ₺".toDisplayText(turkish))
    }
}
