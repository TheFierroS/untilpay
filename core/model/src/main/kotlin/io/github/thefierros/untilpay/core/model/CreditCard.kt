package io.github.thefierros.untilpay.core.model

import java.util.Currency

/**
 * A credit card. Statement amounts and payment plans change every period and
 * live with the statements; this describes the card itself.
 */
data class CreditCard(
    val id: CardId,
    val name: String,
    val currency: Currency,
    /** Day of the month on which a statement is issued. */
    val statementDay: Int,
    /** Day of the month on which the statement payment is due. */
    val dueDay: Int,
) {
    init {
        require(name.isNotBlank()) { "Card name must not be blank" }
        require(statementDay in 1..31) {
            "statementDay must be between 1 and 31, got $statementDay"
        }
        require(dueDay in 1..31) { "dueDay must be between 1 and 31, got $dueDay" }
    }
}
