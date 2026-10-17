package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import io.github.thefierros.untilpay.core.domain.InstallmentPlan
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CategoryId
import io.github.thefierros.untilpay.core.model.IncomeSourceId
import io.github.thefierros.untilpay.core.model.Money
import io.github.thefierros.untilpay.core.model.PaymentSource
import io.github.thefierros.untilpay.core.model.StatementId
import io.github.thefierros.untilpay.core.model.Transaction
import io.github.thefierros.untilpay.core.model.TransactionId
import java.time.Instant
import java.time.ZoneId
import java.util.Currency

private const val EXPENSE = "EXPENSE"
private const val INCOME = "INCOME"
private const val TRANSFER = "TRANSFER"
private const val CARD_PAYMENT = "CARD_PAYMENT"

/** How this transaction changes the balance of account [accountId], in minor units. */
internal fun TransactionEntity.effectOn(accountId: Long): Long = fromRow("transaction $id") {
    require(amountMinor > 0) { "amount must be positive, got $amountMinor" }
    when (type) {
        INCOME -> amountMinor
        EXPENSE, CARD_PAYMENT -> -amountMinor
        TRANSFER -> if (toAccountId == accountId) amountMinor else -amountMinor
        else -> throw IllegalArgumentException("unknown transaction type '$type'")
    }
}

/**
 * The installment plan of this card purchase. Every statement that closed on
 * or after the day of the purchase, in [zone], has billed one installment.
 */
internal fun TransactionEntity.asInstallmentPlan(
    currency: Currency,
    statements: List<CardStatementEntity>,
    zone: ZoneId,
): InstallmentPlan = fromRow("transaction $id") {
    val count = requireNotNull(installmentCount) { "an installment purchase needs a count" }
    val purchaseDay = Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate().toEpochDay()
    val billed = statements.count { it.closingDate >= purchaseDay }
    InstallmentPlan(
        total = Money(amountMinor, currency),
        count = count,
        billedCount = billed.coerceAtMost(count),
    )
}

/**
 * The transaction in this row. Its amount is in [currency], the currency of its
 * account, or of its card for card spending.
 */
internal fun TransactionEntity.asModel(currency: Currency): Transaction =
    fromRow("transaction $id") {
        val transaction = toTransaction(currency)
        require(transaction.asEntity(createdAt) == this) {
            "a $type row has columns of another kind of transaction filled"
        }
        transaction
    }

internal fun Transaction.asEntity(createdAt: Long): TransactionEntity {
    fun row(type: String) = TransactionEntity(
        id = id.value,
        type = type,
        amountMinor = amount.minorUnits,
        occurredAt = occurredAt.toEpochMilli(),
        note = note,
        createdAt = createdAt,
    )
    return when (this) {
        is Transaction.Expense -> when (val source = source) {
            is PaymentSource.FromAccount -> row(EXPENSE).copy(
                accountId = source.accountId.value,
                categoryId = categoryId?.value,
            )

            is PaymentSource.OnCard -> row(EXPENSE).copy(
                cardId = source.cardId.value,
                installmentCount = source.installments,
                categoryId = categoryId?.value,
            )
        }

        is Transaction.Income -> row(INCOME).copy(
            accountId = accountId.value,
            incomeSourceId = incomeSourceId?.value,
        )

        is Transaction.Transfer -> row(TRANSFER).copy(
            accountId = fromAccountId.value,
            toAccountId = toAccountId.value,
        )

        is Transaction.CardPayment -> row(CARD_PAYMENT).copy(
            accountId = fromAccountId.value,
            cardStatementId = statementId.value,
        )
    }
}

private fun TransactionEntity.toTransaction(currency: Currency): Transaction {
    val transactionId = TransactionId(id)
    val money = Money(amountMinor, currency)
    val moment = Instant.ofEpochMilli(occurredAt)
    return when (type) {
        EXPENSE -> Transaction.Expense(
            id = transactionId,
            amount = money,
            occurredAt = moment,
            source = expenseSource(),
            categoryId = categoryId?.let(::CategoryId),
            note = note,
        )

        INCOME -> Transaction.Income(
            id = transactionId,
            amount = money,
            occurredAt = moment,
            accountId = AccountId(needed(accountId, "an account")),
            incomeSourceId = incomeSourceId?.let(::IncomeSourceId),
            note = note,
        )

        TRANSFER -> Transaction.Transfer(
            id = transactionId,
            amount = money,
            occurredAt = moment,
            fromAccountId = AccountId(needed(accountId, "an account to transfer from")),
            toAccountId = AccountId(needed(toAccountId, "an account to transfer to")),
            note = note,
        )

        CARD_PAYMENT -> Transaction.CardPayment(
            id = transactionId,
            amount = money,
            occurredAt = moment,
            fromAccountId = AccountId(needed(accountId, "an account to pay from")),
            statementId = StatementId(needed(cardStatementId, "a statement")),
            note = note,
        )

        else -> throw IllegalArgumentException("unknown transaction type '$type'")
    }
}

private fun TransactionEntity.expenseSource(): PaymentSource {
    val account = accountId
    val card = cardId
    return when {
        account != null && card == null -> PaymentSource.FromAccount(AccountId(account))
        card != null && account == null -> PaymentSource.OnCard(CardId(card), installmentCount)
        else -> throw IllegalArgumentException("an expense needs either an account or a card")
    }
}

private fun <T : Any> TransactionEntity.needed(value: T?, what: String): T =
    requireNotNull(value) { "a $type transaction needs $what" }
