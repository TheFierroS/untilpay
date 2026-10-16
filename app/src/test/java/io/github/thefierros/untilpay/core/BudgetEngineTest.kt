package io.github.thefierros.untilpay.core

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetEngineTest {

    private val today = LocalDate.of(2026, 10, 16)
    private val payday = LocalDate.of(2026, 11, 1)

    private val card = CardState(
        statementBalance = Money.lira(3000),
        minimumPayment = Money.lira(600),
        plannedPayment = Money.lira(1000),
        paidSoFar = Money.ZERO,
        dueDate = LocalDate.of(2026, 10, 28),
        spentSinceStatement = Money.lira(200),
    )

    @Test
    fun `without a card, cash is split across the remaining days`() {
        val result = BudgetEngine.calculate(BudgetInput(today, payday, cash = Money.lira(4800)))

        assertEquals(16L, result.daysLeft)
        assertEquals(Money.lira(300), result.dailyLimit)
    }

    @Test
    fun `planned payment and new card spending are set aside`() {
        val result = BudgetEngine.calculate(
            BudgetInput(today, payday, cash = Money.lira(4800), card = card),
        )

        assertEquals(Money.lira(3600), result.spendable)
        assertEquals(Money.lira(225), result.dailyLimit)
        assertEquals(
            DebtSummary(
                total = Money.lira(3200),
                deferred = Money.lira(2000),
                belowMinimum = false,
            ),
            result.debt,
        )
    }

    @Test
    fun `paying part of the plan does not change the daily limit`() {
        // After paying 1000, cash drops by 1000 and nothing is left to reserve.
        val paid = card.copy(paidSoFar = Money.lira(1000))
        val result = BudgetEngine.calculate(
            BudgetInput(today, payday, cash = Money.lira(3800), card = paid),
        )

        assertEquals(Money.ZERO, result.reserved.cardPayment)
        assertEquals(Money.lira(225), result.dailyLimit)
    }

    @Test
    fun `a plan below the minimum payment is flagged`() {
        val result = BudgetEngine.calculate(
            BudgetInput(
                today,
                payday,
                cash = Money.lira(4800),
                card = card.copy(plannedPayment = Money.lira(400)),
            ),
        )

        assertTrue(result.debt!!.belowMinimum)
    }

    @Test
    fun `payment due after payday is not reserved yet`() {
        val result = BudgetEngine.calculate(
            BudgetInput(
                today,
                payday,
                cash = Money.lira(4800),
                card = card.copy(dueDate = LocalDate.of(2026, 11, 5)),
            ),
        )

        assertEquals(Money.ZERO, result.reserved.cardPayment)
        assertFalse(result.debt!!.belowMinimum)
    }

    @Test
    fun `daily limit never goes negative`() {
        val result = BudgetEngine.calculate(
            BudgetInput(today, payday, cash = Money.lira(100), fixedBeforeIncome = Money.lira(500)),
        )

        assertEquals(Money.lira(-400), result.spendable)
        assertEquals(Money.ZERO, result.dailyLimit)
    }

    @Test
    fun `on payday itself there is still one day to divide by`() {
        val result = BudgetEngine.calculate(BudgetInput(payday, payday, cash = Money.lira(500)))

        assertEquals(1L, result.daysLeft)
        assertEquals(Money.lira(500), result.dailyLimit)
    }
}
