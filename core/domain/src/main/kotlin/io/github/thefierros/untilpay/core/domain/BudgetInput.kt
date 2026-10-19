package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.FixedCost
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
    /** Single-payment card spending made after the latest statement was cut. */
    val spentSinceStatement: Money,
    /** Installment purchases that still have installments to be billed. */
    val installmentPlans: List<InstallmentPlan> = emptyList(),
) {
    init {
        val amounts =
            listOf(statementBalance, minimumPayment, plannedPayment, paidSoFar, spentSinceStatement)
        for (amount in amounts + installmentPlans.map { it.total }) {
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

    /** Everything the next statement will show: new spending and the next installments. */
    val nextStatementCharges: Money
        get() = installmentPlans
            .mapNotNull { it.nextInstallment }
            .fold(spentSinceStatement) { sum, amount -> sum + amount }

    /** Installments that will appear on statements after the next one. */
    val laterInstallments: Money
        get() {
            val zero = Money.zero(card.currency)
            return installmentPlans
                .map { it.unbilled - (it.nextInstallment ?: zero) }
                .fold(zero) { sum, amount -> sum + amount }
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
    /**
     * Costs paid on a set day every month. Every payment from today until the
     * next income is set aside; earlier ones are already out of the balance.
     */
    val fixedCosts: List<FixedCost> = emptyList(),
    /** Money the user sets aside and never wants counted as spendable. */
    val protectedAmount: Money = Money.zero(currency),
) {
    init {
        for (cost in fixedCosts) {
            requireCurrency(cost.amount, currency, "Fixed cost ${cost.name}")
        }
        requireCurrency(protectedAmount, currency, "protectedAmount")
        requireNotNegative(protectedAmount, "protectedAmount")
    }
}
