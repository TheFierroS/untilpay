package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class InstallmentPlanTest {

    private val turkishLira = Currency.getInstance("TRY")

    private fun lira(amount: Long) = Money(amount * 100, turkishLira)
    private fun kurus(amount: Long) = Money(amount, turkishLira)

    @Test
    fun `an evenly divisible total gives equal installments`() {
        val plan = InstallmentPlan(lira(6000), count = 6, billedCount = 0)

        assertEquals((1..6).map { lira(1000) }, (1..6).map { plan.installment(it) })
    }

    @Test
    fun `leftover minor units go to the first installment`() {
        val plan = InstallmentPlan(lira(1000), count = 3, billedCount = 0)

        assertEquals(kurus(33334), plan.installment(1))
        assertEquals(kurus(33333), plan.installment(2))
        assertEquals(kurus(33333), plan.installment(3))
    }

    @Test
    fun `installments always add up to exactly the total`() {
        for (totalMinorUnits in listOf(1L, 99L, 99_999L, 100_001L, 123_456_789L)) {
            for (count in 1..12) {
                val plan = InstallmentPlan(kurus(totalMinorUnits), count, billedCount = 0)
                assertEquals("total $totalMinorUnits in $count", plan.total, plan.unbilled)
            }
        }
    }

    @Test
    fun `a single installment is the whole total`() {
        val plan = InstallmentPlan(lira(750), count = 1, billedCount = 0)

        assertEquals(lira(750), plan.nextInstallment)
    }

    @Test
    fun `the next and unbilled installments follow the billed count`() {
        val plan = InstallmentPlan(lira(6000), count = 6, billedCount = 2)

        assertEquals(lira(1000), plan.nextInstallment)
        assertEquals(lira(4000), plan.unbilled)
    }

    @Test
    fun `a fully billed plan has nothing left`() {
        val plan = InstallmentPlan(lira(6000), count = 6, billedCount = 6)

        assertNull(plan.nextInstallment)
        assertEquals(Money.zero(turkishLira), plan.unbilled)
    }

    @Test
    fun `invalid plans are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlan(Money.zero(turkishLira), count = 3, billedCount = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlan(lira(100), count = 0, billedCount = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlan(
                lira(100),
                count = InstallmentPlan.MAX_INSTALLMENTS + 1,
                billedCount = 0,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlan(lira(100), count = 3, billedCount = -1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            InstallmentPlan(lira(100), count = 3, billedCount = 4)
        }
    }

    @Test
    fun `installment numbers outside the plan are rejected`() {
        val plan = InstallmentPlan(lira(100), count = 3, billedCount = 0)

        assertThrows(IllegalArgumentException::class.java) { plan.installment(0) }
        assertThrows(IllegalArgumentException::class.java) { plan.installment(4) }
    }
}
