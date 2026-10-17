package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asEntity
import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.CardStatementDao
import io.github.thefierros.untilpay.core.database.dao.CreditCardDao
import io.github.thefierros.untilpay.core.database.dao.TransactionDao
import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import io.github.thefierros.untilpay.core.model.PaymentSource
import io.github.thefierros.untilpay.core.model.Transaction
import io.github.thefierros.untilpay.core.model.TransactionId
import java.time.Clock
import java.time.Instant
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomTransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val creditCardDao: CreditCardDao,
    private val cardStatementDao: CardStatementDao,
    private val clock: Clock,
) : TransactionRepository {

    override fun observeBetween(start: Instant, end: Instant): Flow<List<Transaction>> =
        transactionDao.observeBetween(start.toEpochMilli(), end.toEpochMilli()).map { rows ->
            rows.map { row -> row.asModel(currencyOf(row)) }
        }

    override suspend fun record(transaction: Transaction): TransactionId {
        requireMatchingCurrencies(transaction)
        val row = transaction.asEntity(createdAt = clock.millis()).copy(id = 0)
        return TransactionId(transactionDao.insert(row))
    }

    override suspend fun update(transaction: Transaction) {
        val existing = existing(transaction.id)
        requireMatchingCurrencies(transaction)
        transactionDao.update(transaction.asEntity(createdAt = existing.createdAt))
    }

    override suspend fun delete(id: TransactionId) {
        transactionDao.delete(existing(id))
    }

    /**
     * Checks what a transaction alone cannot: that its amount is in the currency
     * of every account, card and statement it touches.
     */
    private suspend fun requireMatchingCurrencies(transaction: Transaction) {
        val touched = when (transaction) {
            is Transaction.Expense -> when (val source = transaction.source) {
                is PaymentSource.FromAccount -> listOf(accountCurrency(source.accountId.value))
                is PaymentSource.OnCard -> listOf(cardCurrency(source.cardId.value))
            }

            is Transaction.Income -> listOf(accountCurrency(transaction.accountId.value))

            is Transaction.Transfer -> listOf(
                accountCurrency(transaction.fromAccountId.value),
                accountCurrency(transaction.toAccountId.value),
            )

            is Transaction.CardPayment -> listOf(
                accountCurrency(transaction.fromAccountId.value),
                statementCurrency(transaction.statementId.value),
            )
        }
        val currency = transaction.amount.currency.currencyCode
        for ((owner, ownerCurrency) in touched) {
            require(ownerCurrency == currency) {
                "$owner is in $ownerCurrency, but the transaction is in $currency"
            }
        }
    }

    private suspend fun accountCurrency(id: Long): Pair<String, String> {
        val account = requireNotNull(accountDao.findById(id)) { "There is no account $id" }
        return "Account $id" to account.currencyCode
    }

    private suspend fun cardCurrency(id: Long): Pair<String, String> {
        val card = requireNotNull(creditCardDao.findById(id)) { "There is no credit card $id" }
        return "Credit card $id" to card.currencyCode
    }

    private suspend fun statementCurrency(id: Long): Pair<String, String> {
        val statement = requireNotNull(cardStatementDao.findById(id)) {
            "There is no card statement $id"
        }
        return "Card statement $id" to cardCurrency(statement.cardId).second
    }

    /** The currency of a stored transaction: that of its account, or of its card. */
    private suspend fun currencyOf(row: TransactionEntity): Currency {
        val code = row.accountId?.let { accountCurrency(it).second }
            ?: row.cardId?.let { cardCurrency(it).second }
            ?: error("Stored transaction ${row.id} has neither an account nor a card")
        return Currency.getInstance(code)
    }

    private suspend fun existing(id: TransactionId): TransactionEntity =
        requireNotNull(transactionDao.findById(id.value)) { "There is no transaction ${id.value}" }
}
