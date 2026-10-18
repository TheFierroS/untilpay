package io.github.thefierros.untilpay.core.model

import java.util.Currency

/** The user's budget settings. */
data class BudgetSettings(
    /** The currency the daily number is calculated in. */
    val currency: Currency,
    /** Money the user never wants counted as spendable. */
    val protectedAmount: Money,
) {
    init {
        require(protectedAmount.currency == currency) {
            "protectedAmount must be in $currency, got ${protectedAmount.currency}"
        }
        require(protectedAmount.minorUnits >= 0) {
            "protectedAmount cannot be negative, got $protectedAmount"
        }
    }
}
