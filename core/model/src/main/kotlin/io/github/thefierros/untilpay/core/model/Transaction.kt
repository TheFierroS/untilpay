package io.github.thefierros.untilpay.core.model

import java.time.Instant

/** Where the money for an expense came from. */
sealed interface PaymentSource {

    /** Paid from an account, in cash or with a debit card. */
    data class FromAccount(val accountId: AccountId) : PaymentSource

    /** Paid with a credit card, in a single payment or in [installments]. */
    data class OnCard(val cardId: CardId, val installments: Int? = null) : PaymentSource {
        init {
            require(installments == null || installments in 2..MAX_INSTALLMENTS) {
                "installments must be null for a single payment or between 2 and " +
                    "$MAX_INSTALLMENTS, got $installments"
            }
        }

        companion object {
            /** A sanity limit against typos such as 600 instead of 6, not a legal limit. */
            const val MAX_INSTALLMENTS = 60
        }
    }
}

/**
 * A movement of money. The amount is always positive; the kind of transaction
 * gives its direction.
 */
sealed interface Transaction {
    val id: TransactionId
    val amount: Money

    /** When the money moved. */
    val occurredAt: Instant

    /** A note from the user, or null. Never blank. */
    val note: String?

    data class Expense(
        override val id: TransactionId,
        override val amount: Money,
        override val occurredAt: Instant,
        val source: PaymentSource,
        val categoryId: CategoryId? = null,
        override val note: String? = null,
    ) : Transaction {
        init {
            requireValid(amount, note)
        }
    }

    data class Income(
        override val id: TransactionId,
        override val amount: Money,
        override val occurredAt: Instant,
        val accountId: AccountId,
        val incomeSourceId: IncomeSourceId? = null,
        override val note: String? = null,
    ) : Transaction {
        init {
            requireValid(amount, note)
        }
    }

    data class Transfer(
        override val id: TransactionId,
        override val amount: Money,
        override val occurredAt: Instant,
        val fromAccountId: AccountId,
        val toAccountId: AccountId,
        override val note: String? = null,
    ) : Transaction {
        init {
            requireValid(amount, note)
            require(fromAccountId != toAccountId) { "A transfer needs two different accounts" }
        }
    }

    /** A payment from an account toward a card statement. */
    data class CardPayment(
        override val id: TransactionId,
        override val amount: Money,
        override val occurredAt: Instant,
        val fromAccountId: AccountId,
        val statementId: StatementId,
        override val note: String? = null,
    ) : Transaction {
        init {
            requireValid(amount, note)
        }
    }
}

private fun requireValid(amount: Money, note: String?) {
    require(amount.isPositive) { "A transaction amount must be positive, got $amount" }
    require(note == null || note.isNotBlank()) { "A note must not be blank; use null for none" }
}
