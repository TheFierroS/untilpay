package io.github.thefierros.untilpay.core.model

import java.time.LocalDate

/**
 * A statement issued for a credit card, as printed by the bank, plus what the
 * user plans to pay toward it.
 *
 * Whether the amounts are in the card's currency is checked where the card is
 * known, when the statement is saved.
 */
data class CardStatement(
    val id: StatementId,
    val cardId: CardId,
    /** The day the statement was cut. */
    val closingDate: LocalDate,
    /** The payment due date printed on the statement. */
    val dueDate: LocalDate,
    /** Debt shown on the statement. */
    val balance: Money,
    /** Minimum payment shown on the statement. */
    val minimumPayment: Money,
    /** What the user plans to pay toward the statement. May be below the minimum. */
    val plannedPayment: Money,
) {
    init {
        require(!dueDate.isBefore(closingDate)) {
            "dueDate ($dueDate) cannot be before closingDate ($closingDate)"
        }
        require(
            minimumPayment.currency == balance.currency &&
                plannedPayment.currency == balance.currency,
        ) { "All amounts of a statement must be in the same currency" }
        require(balance.minorUnits >= 0) { "balance cannot be negative, got $balance" }
        require(minimumPayment.minorUnits >= 0) {
            "minimumPayment cannot be negative, got $minimumPayment"
        }
        require(minimumPayment <= balance) {
            "minimumPayment ($minimumPayment) cannot exceed balance ($balance)"
        }
        require(plannedPayment.minorUnits >= 0) {
            "plannedPayment cannot be negative, got $plannedPayment"
        }
    }
}
