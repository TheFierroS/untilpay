package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.IncomeSource
import io.github.thefierros.untilpay.core.model.Money
import java.time.LocalDate
import java.util.Currency

/** The balance of one account on the budget day, in the account's currency. */
data class AccountBalance(val account: Account, val balance: Money) {
    init {
        requireCurrency(balance, account.currency, "The balance of ${account.name}")
    }
}

/** The state of one credit card for its current statement period. */
data class CardState(
    val card: CreditCard,
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
    init {
        val amounts =
            listOf(statementBalance, minimumPayment, plannedPayment, paidSoFar, spentSinceStatement)
        for (amount in amounts) {
            requireCurrency(amount, card.currency, "Amounts of ${card.name}")
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

/**
 * Everything the budget engine needs to know about the user's money on [today].
 *
 * Only accounts, cards and income sources in the budget [currency] take part in
 * the calculation. Accounts and cards in other currencies are reported back as
 * excluded until exchange rates are supported.
 */
data class BudgetInput(
    val today: LocalDate,
    /** The budget currency. */
    val currency: Currency,
    val accounts: List<AccountBalance>,
    val cards: List<CardState> = emptyList(),
    val incomeSources: List<IncomeSource>,
    /** Fixed costs (subscriptions, transit, ...) due before the next income. */
    val fixedCosts: Money = Money.zero(currency),
    /** Money the user sets aside and never wants counted as spendable. */
    val protectedAmount: Money = Money.zero(currency),
) {
    init {
        requireCurrency(fixedCosts, currency, "fixedCosts")
        requireCurrency(protectedAmount, currency, "protectedAmount")
        requireNotNegative(fixedCosts, "fixedCosts")
        requireNotNegative(protectedAmount, "protectedAmount")
    }
}
