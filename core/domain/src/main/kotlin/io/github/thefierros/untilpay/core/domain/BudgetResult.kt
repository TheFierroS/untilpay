package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.Money
import java.time.LocalDate

/** Everything set aside from the cash before the rest is split into days. */
data class Reserved(
    val cardPayments: Money,
    /**
     * What the next card statements will show: new spending and the next
     * installments. Set aside right away, so a card can't become an escape
     * hatch from the daily limit.
     */
    val nextStatementCharges: Money,
    val fixedCosts: Money,
    val protectedAmount: Money,
) {
    val total: Money get() = cardPayments + nextStatementCharges + fixedCosts + protectedAmount
}

/** The debt situation of one card. */
data class CardDebt(
    val cardId: CardId,
    /** Everything owed on the card, including installments not billed yet. */
    val total: Money,
    val deferred: Money,
    val belowMinimum: Boolean,
    /** Unpaid part of the minimum payment once the due date has passed. */
    val overdue: Money,
    /** Installments that will appear on statements after the next one. */
    val laterInstallments: Money,
) {
    val isOverdue: Boolean get() = overdue.isPositive
}

data class BudgetResult(
    val nextIncomeDate: LocalDate,
    /** Always at least 1: today counts as a day to spend. */
    val daysLeft: Long,
    val reserved: Reserved,
    val spendable: Money,
    val dailyLimit: Money,
    val cardDebts: List<CardDebt>,
    /** Accounts left out because their currency differs from the budget currency. */
    val excludedAccounts: List<AccountId>,
    /** Cards left out because their currency differs from the budget currency. */
    val excludedCards: List<CardId>,
)

/** What the budget engine can tell about the user's situation. */
sealed interface BudgetOutcome {

    /** A daily limit was calculated for the days left until the next income. */
    data class Calculated(val result: BudgetResult) : BudgetOutcome

    /**
     * No income in the budget currency is expected anymore, so there is no
     * period to budget for. The user has to add or update an income source.
     */
    data object NoUpcomingIncome : BudgetOutcome
}
