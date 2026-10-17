package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Money
import java.time.LocalDate
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetEngineTest {

    private val turkishLira = Currency.getInstance("TRY")
    private val zero = Money.zero(turkishLira)

    private fun lira(amount: Long) = Money(amount * 100, turkishLira)

    private val today = LocalDate.of(2026, 10, 16)
    private val payday = LocalDate.of(2026, 11, 1)

    private val card = CardState(
        statementBalance = lira(3000),
        minimumPayment = lira(600),
        plannedPayment = lira(1000),
        paidSoFar = zero,
        dueDate = LocalDate.of(2026, 10, 28),
        spentSinceStatement = lira(200),
    )

    /** Runs the engine and fails with a clear message if no limit was calculated. */
    private fun BudgetInput.calculated(): BudgetResult {
        val outcome = BudgetEngine.calculate(this)
        check(outcome is BudgetOutcome.Calculated) { "Expected a calculated budget, got $outcome" }
        return outcome.result
    }

    @Test
    fun `without a card, cash is split across the remaining days`() {
        val result = BudgetInput(today, payday, cash = lira(4800)).calculated()

        assertEquals(16L, result.daysLeft)
        assertEquals(lira(300), result.dailyLimit)
    }

    @Test
    fun `planned payment and new card spending are set aside`() {
        val result = BudgetInput(today, payday, cash = lira(4800), card = card).calculated()

        assertEquals(lira(3600), result.spendable)
        assertEquals(lira(225), result.dailyLimit)
        assertEquals(
            DebtSummary(
                total = lira(3200),
                deferred = lira(2000),
                belowMinimum = false,
                overdue = zero,
            ),
            result.debt,
        )
    }

    @Test
    fun `paying part of the plan does not change the daily limit`() {
        // After paying 1000, cash drops by 1000 and nothing is left to reserve.
        val paid = card.copy(paidSoFar = lira(1000))
        val result = BudgetInput(today, payday, cash = lira(3800), card = paid).calculated()

        assertEquals(zero, result.reserved.cardPayment)
        assertEquals(lira(225), result.dailyLimit)
    }

    @Test
    fun `a plan below the minimum payment is flagged`() {
        val lowPlan = card.copy(plannedPayment = lira(400))
        val result = BudgetInput(today, payday, cash = lira(4800), card = lowPlan).calculated()

        assertTrue(checkNotNull(result.debt).belowMinimum)
    }

    @Test
    fun `payment due after payday is not reserved yet`() {
        val laterDue = card.copy(dueDate = LocalDate.of(2026, 11, 5))
        val result = BudgetInput(today, payday, cash = lira(4800), card = laterDue).calculated()

        assertEquals(zero, result.reserved.cardPayment)
        assertFalse(checkNotNull(result.debt).belowMinimum)
    }

    @Test
    fun `daily limit never goes negative`() {
        val result = BudgetInput(
            today,
            payday,
            cash = lira(100),
            fixedBeforeIncome = lira(500),
        ).calculated()

        assertEquals(lira(-400), result.spendable)
        assertEquals(zero, result.dailyLimit)
    }

    @Test
    fun `on payday the next income date has to be updated`() {
        val outcome = BudgetEngine.calculate(BudgetInput(payday, payday, cash = lira(500)))

        assertEquals(BudgetOutcome.NextIncomeDateReached(payday), outcome)
    }

    @Test
    fun `a next income date in the past has to be updated`() {
        val afterPayday = LocalDate.of(2026, 11, 3)
        val outcome = BudgetEngine.calculate(BudgetInput(afterPayday, payday, cash = lira(500)))

        assertEquals(BudgetOutcome.NextIncomeDateReached(payday), outcome)
    }

    @Test
    fun `an unpaid minimum after the due date is reserved and flagged as overdue`() {
        val afterDueDate = LocalDate.of(2026, 10, 30)
        val partlyPaid = card.copy(paidSoFar = lira(200))
        val result = BudgetInput(afterDueDate, payday, cash = lira(4800), card = partlyPaid)
            .calculated()
        val debt = checkNotNull(result.debt)

        // 600 minimum - 200 paid = 400 overdue; 4800 - 400 - 200 new spending = 4200 over 2 days.
        assertEquals(lira(400), result.reserved.cardPayment)
        assertEquals(lira(400), debt.overdue)
        assertTrue(debt.isOverdue)
        assertEquals(lira(2100), result.dailyLimit)
    }

    @Test
    fun `nothing is reserved after the due date once the minimum is paid`() {
        val afterDueDate = LocalDate.of(2026, 10, 30)
        val minimumPaid = card.copy(paidSoFar = lira(600))
        val result = BudgetInput(afterDueDate, payday, cash = lira(4800), card = minimumPaid)
            .calculated()

        assertEquals(zero, result.reserved.cardPayment)
        assertFalse(checkNotNull(result.debt).isOverdue)
    }

    @Test
    fun `a payment due today is reserved but not yet overdue`() {
        val result = BudgetInput(card.dueDate, payday, cash = lira(4800), card = card)
            .calculated()

        assertEquals(lira(1000), result.reserved.cardPayment)
        assertFalse(checkNotNull(result.debt).isOverdue)
    }

    @Test
    fun `negative card amounts are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            card.copy(paidSoFar = lira(-1))
        }
    }

    @Test
    fun `a minimum payment above the statement balance is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            card.copy(minimumPayment = lira(3001))
        }
    }

    @Test
    fun `negative fixed costs are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            BudgetInput(today, payday, cash = lira(100), fixedBeforeIncome = lira(-5))
        }
    }

    @Test
    fun `card amounts in mixed currencies are rejected`() {
        val euros = Money(100, Currency.getInstance("EUR"))
        assertThrows(IllegalArgumentException::class.java) {
            card.copy(paidSoFar = euros)
        }
    }

    @Test
    fun `a card in a different currency than the cash is rejected`() {
        val euro = Money.zero(Currency.getInstance("EUR"))
        val euroCard = CardState(euro, euro, euro, euro, card.dueDate, euro)
        assertThrows(IllegalArgumentException::class.java) {
            BudgetInput(today, payday, cash = lira(100), card = euroCard)
        }
    }

    @Test
    fun `fixed costs in a different currency than the cash are rejected`() {
        val euros = Money(500, Currency.getInstance("EUR"))
        assertThrows(IllegalArgumentException::class.java) {
            BudgetInput(today, payday, cash = lira(100), fixedBeforeIncome = euros)
        }
    }
}
