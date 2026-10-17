package io.github.thefierros.untilpay.core.model

import java.util.Currency

/** What kind of place an account is. */
enum class AccountType {
    /** A bank account, such as a checking account with a debit card. */
    BANK,

    /** Physical cash, such as the money in a wallet. */
    CASH,
}

/**
 * A place where money sits. Its balance is deliberately not stored here: it is
 * derived from balance snapshots and the transactions after them, so it can
 * never drift out of sync with the history it comes from.
 */
data class Account(
    val id: AccountId,
    val name: String,
    val type: AccountType,
    val currency: Currency,
    /** Whether the balance of this account counts toward the daily limit. */
    val includeInBudget: Boolean,
) {
    init {
        require(name.isNotBlank()) { "Account name must not be blank" }
    }
}
