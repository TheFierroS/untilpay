package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Money
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Currency

/** Credit card state for the current statement period. All amounts share one currency. */
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
) {
    /** The currency every amount of this card is in. */
    val currency: Currency get() = statementBalance.currency

    init {
        for (amount in listOf(minimumPayment, plannedPayment, paidSoFar, spentSinceStatement)) {
            requireCurrency(amount, currency, "Card amounts")
        }
        requireNotNegative(statementBalance, "statementBalance")
        requireNotNegative(minimumPayment, "minimumPayment")
        requireNotNegative(plannedPayment, "plannedPayment")
        requireNotNegative(paidSoFar, "paidSoFar")
        requireNotNegative(spentSinceStatement, "spentSinceStatement")
        require(minimumPayment <= statementBalance) {
            "minimumPayment ($minimumPayment) cannot exceed statementBalance ($statementBalance)"
        }
    }
}

data class BudgetInput(
    val today: LocalDate,
    val nextIncomeDate: LocalDate,
    /** Cash on hand, in the budget currency. May be negative when overdrawn. */
    val cash: Money,
    val card: CardState? = null,
    /** Fixed costs (subscriptions, transit, ...) due before the next income. */
    val fixedBeforeIncome: Money = Money.zero(cash.currency),
) {
    /** The budget currency: every amount in the input must be in it. */
    val currency: Currency get() = cash.currency

    init {
        requireCurrency(fixedBeforeIncome, currency, "fixedBeforeIncome")
        if (card != null) requireCurrency(card.statementBalance, currency, "The card")
        requireNotNegative(fixedBeforeIncome, "fixedBeforeIncome")
    }
}

data class Reserved(val cardPayment: Money, val newCardSpending: Money, val fixed: Money)

data class DebtSummary(
    val total: Money,
    val deferred: Money,
    val belowMinimum: Boolean,
    /** Unpaid part of the minimum payment once the due date has passed. */
    val overdue: Money,
) {
    val isOverdue: Boolean get() = overdue.isPositive
}

data class BudgetResult(
    /** Always at least 1: today counts as a day to spend. */
    val daysLeft: Long,
    val reserved: Reserved,
    val spendable: Money,
    val dailyLimit: Money,
    val debt: DebtSummary?,
)

/** What the budget engine can tell about the user's situation. */
sealed interface BudgetOutcome {

    /** A daily limit was calculated for the days left until the next income. */
    data class Calculated(val result: BudgetResult) : BudgetOutcome

    /**
     * The next income date is today or already in the past, so there is no
     * period left to budget for. The user has to confirm the income and set
     * the following income date before a new limit can be calculated.
     */
    data class NextIncomeDateReached(val nextIncomeDate: LocalDate) : BudgetOutcome
}

/**
 * Turns the user's money situation into a single daily spending limit.
 *
 * Pure and deterministic: same input, same output. The AI layer never
 * calculates anything; it only talks about what this engine produces.
 */
object BudgetEngine {

    fun calculate(input: BudgetInput): BudgetOutcome {
        if (!input.nextIncomeDate.isAfter(input.today)) {
            return BudgetOutcome.NextIncomeDateReached(input.nextIncomeDate)
        }

        val daysLeft = ChronoUnit.DAYS.between(input.today, input.nextIncomeDate)
        val zero = Money.zero(input.currency)
        val card = input.card

        val cardPayment = card?.reservation(input.today, input.nextIncomeDate) ?: zero

        // New card spending is set aside immediately,
        // so the card can't become an escape hatch from the daily limit.
        val newCardSpending = card?.spentSinceStatement ?: zero

        val spendable = input.cash - cardPayment - newCardSpending - input.fixedBeforeIncome
        val dailyLimit = (spendable / daysLeft).coerceAtLeast(zero)

        return BudgetOutcome.Calculated(
            BudgetResult(
                daysLeft = daysLeft,
                reserved = Reserved(cardPayment, newCardSpending, input.fixedBeforeIncome),
                spendable = spendable,
                dailyLimit = dailyLimit,
                debt = card?.toDebtSummary(input.today),
            ),
        )
    }

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
        val zero = Money.zero(currency)
        return when {
            dueDate.isBefore(today) -> overdueAmount(today)
            dueDate.isBefore(nextIncome) -> (plannedPayment - paidSoFar).coerceAtLeast(zero)
            else -> zero
        }
    }

    private fun CardState.overdueAmount(today: LocalDate): Money {
        val zero = Money.zero(currency)
        if (!dueDate.isBefore(today)) return zero
        return (minimumPayment - paidSoFar).coerceAtLeast(zero)
    }

    private fun CardState.toDebtSummary(today: LocalDate): DebtSummary {
        val effectivePayment = maxOf(plannedPayment, paidSoFar)
        return DebtSummary(
            total = statementBalance - paidSoFar + spentSinceStatement,
            deferred = (statementBalance - effectivePayment).coerceAtLeast(Money.zero(currency)),
            belowMinimum = effectivePayment < minimumPayment,
            overdue = overdueAmount(today),
        )
    }
}

private fun requireCurrency(amount: Money, expected: Currency, name: String) {
    require(amount.currency == expected) {
        "$name must be in ${expected.currencyCode}, got ${amount.currency.currencyCode}"
    }
}

private fun requireNotNegative(amount: Money, name: String) {
    require(!amount.isNegative) { "$name must not be negative, got $amount" }
}
