package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.Transaction
import io.github.thefierros.untilpay.core.model.TransactionId
import java.time.Instant
import kotlinx.coroutines.flow.Flow

/** Records, corrects and lists movements of money. */
interface TransactionRepository {

    /** Transactions from [start] (inclusive) to [end] (exclusive), newest first. */
    fun observeBetween(start: Instant, end: Instant): Flow<List<Transaction>>

    /**
     * Records a transaction and returns its id; the id it carries is ignored.
     * Its amount must be in the currency of every account, card and statement
     * it touches.
     */
    suspend fun record(transaction: Transaction): TransactionId

    /** Saves a corrected transaction. The time it was first entered is kept. */
    suspend fun update(transaction: Transaction)

    /** Deletes a transaction entered by mistake. */
    suspend fun delete(id: TransactionId)
}
