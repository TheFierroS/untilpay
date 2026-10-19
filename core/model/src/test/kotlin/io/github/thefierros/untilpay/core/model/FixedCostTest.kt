package io.github.thefierros.untilpay.core.model

import java.time.LocalDate
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class FixedCostTest {

    private val lira = Currency.getInstance("TRY")

    private fun rent(dayOfMonth: Int) =
        FixedCost(FixedCostId(1), "Rent", Money(1_590_00, lira), dayOfMonth)

    @Test
    fun `due dates run from the first day up to but not including the last`() {
        val dueDates = rent(
            15,
        ).dueDatesBetween(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 12, 15))

        assertEquals(listOf(LocalDate.of(2026, 10, 15), LocalDate.of(2026, 11, 15)), dueDates)
    }

    @Test
    fun `day 31 falls on the last day of a shorter month`() {
        val dueDates = rent(
            31,
        ).dueDatesBetween(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 1))

        assertEquals(listOf(LocalDate.of(2026, 11, 30)), dueDates)
    }

    @Test
    fun `invalid fixed costs are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            FixedCost(FixedCostId(1), " ", Money(100, lira), dayOfMonth = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FixedCost(FixedCostId(1), "Rent", Money(0, lira), dayOfMonth = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FixedCost(FixedCostId(1), "Rent", Money(100, lira), dayOfMonth = 32)
        }
    }
}
