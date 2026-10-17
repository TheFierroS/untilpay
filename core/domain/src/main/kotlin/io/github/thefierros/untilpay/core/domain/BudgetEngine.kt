package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Currency

/**
 * Turns the user's money situation into a single daily spending limit.
 *
 * Pure and deterministic: same input, same output. The AI layer never
 * calculates anything; it only talks about what this engine produces.
 */
object BudgetEngine {

    fun calculate(input: BudgetInput): BudgetOutcome {
        val nextIncomeDate = nextIncomeDate(input) ?: return BudgetOutcome.NoUpcomingIncome

        val (budgetAccounts, foreignAccounts) = input.accounts
            .filter { it.account.includeInBudget }
            .partition { it.account.currency == input.currency }
        val (budgetCards, foreignCards) = input.cards
            .partition { it.card.currency == input.currency }

        val cash = budgetAccounts.map { it.balance }.total(input.currency)
        val reserved = Reserved(
            cardPayments = budgetCards
                .map { it.reservation(input.today, nextIncomeDate) }
                .total(input.currency),
            // New card spending is set aside immediately,
            // so a card can't become an escape hatch from the daily limit.
            newCardSpending = budgetCards.map { it.spentSinceStatement }.total(input.currency),
            fixedCosts = input.fixedCosts,
            protectedAmount = input.protectedAmount,
        )
        val spendable = cash - reserved.total
        val daysLeft = ChronoUnit.DAYS.between(input.today, nextIncomeDate)
        val dailyLimit = (spendable / daysLeft).coerceAtLeast(Money.zero(input.currency))

        return BudgetOutcome.Calculated(
            BudgetResult(
                nextIncomeDate = nextIncomeDate,
                daysLeft = daysLeft,
                reserved = reserved,
                spendable = spendable,
                dailyLimit = dailyLimit,
                cardDebts = budgetCards.map { it.debt(input.today) },
                excludedAccounts = foreignAccounts.map { it.account.id },
                excludedCards = foreignCards.map { it.card.id },
            ),
        )
    }

    /**
     * The earliest upcoming income in the budget currency. Income in another
     * currency does not refill the budget, so it does not end the period.
     */
    private fun nextIncomeDate(input: BudgetInput): LocalDate? = input.incomeSources
        .filter { it.expectedAmount.currency == input.currency }
        .mapNotNull { it.schedule.nextAfter(input.today) }
        .minOrNull()

    /**
     * How much of the current cash has to be set aside for the card.
     *
     * - Due date passed: only the unpaid part of the minimum payment, which is
     *   overdue and accrues late interest. Anything above the minimum has
     *   already rolled over into the next statement.
     * - Due before the next income: the rest of the planned payment.
     * - Due after the next income: nothing yet; the next income will cover it.
     */
    private fun CardState.reservation(today: LocalDate, nextIncome: LocalDate): Money {
        val zero = Money.zero(card.currency)
        return when {
            dueDate.isBefore(today) -> overdueAmount(today)
            dueDate.isBefore(nextIncome) -> (plannedPayment - paidSoFar).coerceAtLeast(zero)
            else -> zero
        }
    }

    private fun CardState.overdueAmount(today: LocalDate): Money {
        val zero = Money.zero(card.currency)
        if (!dueDate.isBefore(today)) return zero
        return (minimumPayment - paidSoFar).coerceAtLeast(zero)
    }

    private fun CardState.debt(today: LocalDate): CardDebt {
        val zero = Money.zero(card.currency)
        val effectivePayment = maxOf(plannedPayment, paidSoFar)
        return CardDebt(
            cardId = card.id,
            total = statementBalance - paidSoFar + spentSinceStatement,
            deferred = (statementBalance - effectivePayment).coerceAtLeast(zero),
            belowMinimum = effectivePayment < minimumPayment,
            overdue = overdueAmount(today),
        )
    }

    private fun List<Money>.total(currency: Currency): Money =
        fold(Money.zero(currency)) { sum, amount -> sum + amount }
}
