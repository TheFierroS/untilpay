package io.github.thefierros.untilpay.core.model

import java.util.Currency
import org.junit.Assert.assertThrows
import org.junit.Test

class BudgetSettingsTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")

    @Test
    fun `a negative or foreign protected amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BudgetSettings(lira, Money(-1, lira))
        }
        assertThrows(IllegalArgumentException::class.java) {
            BudgetSettings(lira, Money(100, euro))
        }
    }
}
