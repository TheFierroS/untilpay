package io.github.thefierros.untilpay.core

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Credit card state for the current statement period. */
data class CardState(
    /** Debt shown on the latest statement. */
    val statementBalance: Money,
    /** Minimum payment shown on the latest statement. */
    val minimumPayment: Money,
    /** What the user plans to pay toward the latest statement. */
    val plannedPayment: Money,
    /** What has already been paid toward the latest statement. */
    val paidSoFar: Money,
    /** Payment due date of the latest statement. */
    val dueDate: LocalDate,
    /** Card spending made after the latest statement was cut. */
    val spentSinceStatement: Money,
)

data class BudgetInput(
    val today: LocalDate,
    val nextIncomeDate: LocalDate,
    val cash: Money,
    val card: CardState? = null,
    /** Fixed costs (subscriptions, transit, ...) due before the next income. */
    val fixedBeforeIncome: Money = Money.ZERO,
)

data class Reserved(
    val cardPayment: Money,
    val newCardSpending: Money,
    val fixed: Money,
)

data class DebtSummary(
    val total: Money,
    val deferred: Money,
    val belowMinimum: Boolean,
)

data class BudgetResult(
    val daysLeft: Long,
    val reserved: Reserved,
    val spendable: Money,
    val dailyLimit: Money,
    val debt: DebtSummary?,
)

/**
 * Turns the user's money situation into a single daily spending limit.
 *
 * Pure and deterministic: same input, same output. The AI layer never
 * calculates anything; it only talks about what this engine produces.
 */
object BudgetEngine {

    fun calculate(input: BudgetInput): BudgetResult {
        val daysLeft = ChronoUnit.DAYS
            .between(input.today, input.nextIncomeDate)
            .coerceAtLeast(1)

        val card = input.card

        val cardPayment =
            if (card != null && card.isDueBetween(input.today, input.nextIncomeDate)) {
                (card.plannedPayment - card.paidSoFar).coerceAtLeast(Money.ZERO)
            } else {
                Money.ZERO
            }

        // New card spending is set aside immediately,
        // so the card can't become an escape hatch from the daily limit.
        val newCardSpending = card?.spentSinceStatement ?: Money.ZERO

        val spendable = input.cash - cardPayment - newCardSpending - input.fixedBeforeIncome
        val dailyLimit = (spendable / daysLeft).coerceAtLeast(Money.ZERO)

        return BudgetResult(
            daysLeft = daysLeft,
            reserved = Reserved(cardPayment, newCardSpending, input.fixedBeforeIncome),
            spendable = spendable,
            dailyLimit = dailyLimit,
            debt = card?.toDebtSummary(),
        )
    }

    private fun CardState.isDueBetween(today: LocalDate, nextIncome: LocalDate) =
        !dueDate.isBefore(today) && dueDate.isBefore(nextIncome)

    private fun CardState.toDebtSummary(): DebtSummary {
        val effectivePayment = maxOf(plannedPayment, paidSoFar)
        return DebtSummary(
            total = statementBalance - paidSoFar + spentSinceStatement,
            deferred = (statementBalance - effectivePayment).coerceAtLeast(Money.ZERO),
            belowMinimum = effectivePayment < minimumPayment,
        )
    }
}