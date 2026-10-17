package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.AccountType
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.IncomeSource
import io.github.thefierros.untilpay.core.model.IncomeSourceId
import io.github.thefierros.untilpay.core.model.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetEngineTest {

    private val turkishLira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")
    private val zero = Money.zero(turkishLira)

    private fun lira(amount: Long) = Money(amount * 100, turkishLira)

    /** A Friday; the monthly allowance below next arrives on 1 November, 16 days later. */
    private val today = LocalDate.of(2026, 10, 16)

    private val allowance = IncomeSource(
        IncomeSourceId(1),
        "Allowance",
        lira(8000),
        IncomeSchedule.Monthly(dayOfMonth = 1),
        AccountId(1),
    )

    private val card = CardState(
        card = CreditCard(CardId(1), "Bankkart", turkishLira, statementDay = 13, dueDay = 28),
        statementBalance = lira(3000),
        minimumPayment = lira(600),
        plannedPayment = lira(1000),
        paidSoFar = zero,
        dueDate = LocalDate.of(2026, 10, 28),
        spentSinceStatement = lira(200),
    )

    private fun account(id: Long, balance: Money, includeInBudget: Boolean = true) = AccountBalance(
        Account(
            id = AccountId(id),
            name = "Account $id",
            type = AccountType.BANK,
            currency = balance.currency,
            includeInBudget = includeInBudget,
        ),
        balance,
    )

    /** A budget in lira with one account holding [cash] lira and the monthly allowance. */
    private fun budget(
        cash: Long,
        cards: List<CardState> = emptyList(),
        today: LocalDate = this.today,
        fixedCosts: Money = zero,
        protectedAmount: Money = zero,
    ) = BudgetInput(
        today = today,
        currency = turkishLira,
        accounts = listOf(account(1, lira(cash))),
        cards = cards,
        incomeSources = listOf(allowance),
        fixedCosts = fixedCosts,
        protectedAmount = protectedAmount,
    )

    /** Runs the engine and fails with a clear message if no limit was calculated. */
    private fun BudgetInput.calculated(): BudgetResult {
        val outcome = BudgetEngine.calculate(this)
        check(outcome is BudgetOutcome.Calculated) { "Expected a calculated budget, got $outcome" }
        return outcome.result
    }

    @Test
    fun `without a card, cash is split across the days until the next income`() {
        val result = budget(cash = 4800).calculated()

        assertEquals(LocalDate.of(2026, 11, 1), result.nextIncomeDate)
        assertEquals(16L, result.daysLeft)
        assertEquals(lira(300), result.dailyLimit)
    }

    @Test
    fun `planned payment and new card spending are set aside`() {
        val result = budget(cash = 4800, cards = listOf(card)).calculated()

        assertEquals(lira(3600), result.spendable)
        assertEquals(lira(225), result.dailyLimit)
        assertEquals(
            listOf(
                CardDebt(
                    cardId = CardId(1),
                    total = lira(3200),
                    deferred = lira(2000),
                    belowMinimum = false,
                    overdue = zero,
                ),
            ),
            result.cardDebts,
        )
    }

    @Test
    fun `paying part of the plan does not change the daily limit`() {
        // After paying 1000, cash drops by 1000 and nothing is left to reserve.
        val paid = card.copy(paidSoFar = lira(1000))
        val result = budget(cash = 3800, cards = listOf(paid)).calculated()

        assertEquals(zero, result.reserved.cardPayments)
        assertEquals(lira(225), result.dailyLimit)
    }

    @Test
    fun `a plan below the minimum payment is flagged`() {
        val lowPlan = card.copy(plannedPayment = lira(400))
        val result = budget(cash = 4800, cards = listOf(lowPlan)).calculated()

        assertTrue(result.cardDebts.single().belowMinimum)
    }

    @Test
    fun `payment due after the next income is not reserved yet`() {
        val laterDue = card.copy(dueDate = LocalDate.of(2026, 11, 5))
        val result = budget(cash = 4800, cards = listOf(laterDue)).calculated()

        assertEquals(zero, result.reserved.cardPayments)
        assertFalse(result.cardDebts.single().belowMinimum)
    }

    @Test
    fun `daily limit never goes negative`() {
        val result = budget(cash = 100, fixedCosts = lira(500)).calculated()

        assertEquals(lira(-400), result.spendable)
        assertEquals(zero, result.dailyLimit)
    }

    @Test
    fun `on payday the period runs until the following income`() {
        val payday = LocalDate.of(2026, 11, 1)
        val result = budget(cash = 9000, today = payday).calculated()

        assertEquals(LocalDate.of(2026, 12, 1), result.nextIncomeDate)
        assertEquals(30L, result.daysLeft)
        assertEquals(lira(300), result.dailyLimit)
    }

    @Test
    fun `the earliest upcoming income ends the period`() {
        val partTimeJob = IncomeSource(
            IncomeSourceId(2),
            "Part-time job",
            lira(1500),
            IncomeSchedule.Weekly(DayOfWeek.WEDNESDAY),
            AccountId(1),
        )
        val input = budget(cash = 4800).copy(incomeSources = listOf(allowance, partTimeJob))
        val result = input.calculated()

        // The next Wednesday after Friday 16 October is 21 October.
        assertEquals(LocalDate.of(2026, 10, 21), result.nextIncomeDate)
        assertEquals(5L, result.daysLeft)
    }

    @Test
    fun `without any upcoming income there is no period to budget for`() {
        val pastOnly = allowance.copy(schedule = IncomeSchedule.Once(LocalDate.of(2026, 10, 1)))
        val input = budget(cash = 4800).copy(incomeSources = listOf(pastOnly))

        assertEquals(BudgetOutcome.NoUpcomingIncome, BudgetEngine.calculate(input))
    }

    @Test
    fun `an unpaid minimum after the due date is reserved and flagged as overdue`() {
        val afterDueDate = LocalDate.of(2026, 10, 30)
        val partlyPaid = card.copy(paidSoFar = lira(200))
        val result = budget(cash = 4800, cards = listOf(partlyPaid), today = afterDueDate)
            .calculated()
        val debt = result.cardDebts.single()

        // 600 minimum - 200 paid = 400 overdue; 4800 - 400 - 200 new spending = 4200 over 2 days.
        assertEquals(lira(400), result.reserved.cardPayments)
        assertEquals(lira(400), debt.overdue)
        assertTrue(debt.isOverdue)
        assertEquals(lira(2100), result.dailyLimit)
    }

    @Test
    fun `nothing is reserved after the due date once the minimum is paid`() {
        val afterDueDate = LocalDate.of(2026, 10, 30)
        val minimumPaid = card.copy(paidSoFar = lira(600))
        val result = budget(cash = 4800, cards = listOf(minimumPaid), today = afterDueDate)
            .calculated()

        assertEquals(zero, result.reserved.cardPayments)
        assertFalse(result.cardDebts.single().isOverdue)
    }

    @Test
    fun `a payment due today is reserved but not yet overdue`() {
        val result = budget(cash = 4800, cards = listOf(card), today = card.dueDate).calculated()

        assertEquals(lira(1000), result.reserved.cardPayments)
        assertFalse(result.cardDebts.single().isOverdue)
    }

    @Test
    fun `cash is the sum of every account included in the budget`() {
        val input = budget(cash = 3000).copy(
            accounts = listOf(
                account(1, lira(3000)),
                account(2, lira(1800)),
                account(3, lira(50000), includeInBudget = false),
            ),
        )
        val result = input.calculated()

        // 3000 + 1800 = 4800 over 16 days; the third account stays out of the budget.
        assertEquals(lira(300), result.dailyLimit)
        assertTrue(result.excludedAccounts.isEmpty())
    }

    @Test
    fun `every card is set aside and reported separately`() {
        val secondCard = CardState(
            card = CreditCard(CardId(2), "Second card", turkishLira, statementDay = 1, dueDay = 20),
            statementBalance = lira(500),
            minimumPayment = lira(100),
            plannedPayment = lira(500),
            paidSoFar = zero,
            dueDate = LocalDate.of(2026, 10, 20),
            spentSinceStatement = lira(300),
        )
        val result = budget(cash = 4800, cards = listOf(card, secondCard)).calculated()

        // Planned 1000 + 500, new spending 200 + 300; 4800 - 2000 = 2800 over 16 days.
        assertEquals(lira(1500), result.reserved.cardPayments)
        assertEquals(lira(500), result.reserved.newCardSpending)
        assertEquals(lira(175), result.dailyLimit)
        assertEquals(listOf(CardId(1), CardId(2)), result.cardDebts.map { it.cardId })
    }

    @Test
    fun `protected money is never counted as spendable`() {
        val result = budget(cash = 4800, protectedAmount = lira(1600)).calculated()

        assertEquals(lira(1600), result.reserved.protectedAmount)
        assertEquals(lira(200), result.dailyLimit)
    }

    @Test
    fun `accounts and cards in another currency are left out and reported`() {
        val euroZero = Money.zero(euro)
        val euroCard = CardState(
            card = CreditCard(CardId(2), "Euro card", euro, statementDay = 1, dueDay = 20),
            statementBalance = Money(50000, euro),
            minimumPayment = euroZero,
            plannedPayment = euroZero,
            paidSoFar = euroZero,
            dueDate = LocalDate.of(2026, 10, 20),
            spentSinceStatement = euroZero,
        )
        val input = budget(cash = 4800).copy(
            accounts = listOf(account(1, lira(4800)), account(2, Money(100000, euro))),
            cards = listOf(euroCard),
        )
        val result = input.calculated()

        assertEquals(lira(300), result.dailyLimit)
        assertEquals(listOf(AccountId(2)), result.excludedAccounts)
        assertEquals(listOf(CardId(2)), result.excludedCards)
        assertTrue(result.cardDebts.isEmpty())
    }

    @Test
    fun `income in another currency does not end the period`() {
        val scholarship = IncomeSource(
            IncomeSourceId(2),
            "Scholarship",
            Money(50000, euro),
            IncomeSchedule.Weekly(DayOfWeek.WEDNESDAY),
            AccountId(2),
        )
        val input = budget(cash = 4800).copy(incomeSources = listOf(allowance, scholarship))

        assertEquals(LocalDate.of(2026, 11, 1), input.calculated().nextIncomeDate)
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
    fun `negative fixed costs and protected money are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            budget(cash = 100, fixedCosts = lira(-5))
        }
        assertThrows(IllegalArgumentException::class.java) {
            budget(cash = 100, protectedAmount = lira(-5))
        }
    }

    @Test
    fun `card amounts in another currency than the card are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            card.copy(paidSoFar = Money(100, euro))
        }
    }

    @Test
    fun `an account balance in another currency than the account is rejected`() {
        val liraAccount = account(1, lira(100)).account
        assertThrows(IllegalArgumentException::class.java) {
            AccountBalance(liraAccount, Money(100, euro))
        }
    }

    @Test
    fun `fixed costs in another currency than the budget are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            budget(cash = 100, fixedCosts = Money(500, euro))
        }
    }
}
