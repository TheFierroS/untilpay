package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    /** Inserts a new transaction and returns its generated id. */
    @Insert
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    /** Removes a transaction entered by mistake. */
    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun findById(id: Long): TransactionEntity?

    /** Transactions from [start] (inclusive) to [end] (exclusive), newest first. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE occurred_at >= :start AND occurred_at < :end
        ORDER BY occurred_at DESC, id DESC
        """,
    )
    fun observeBetween(start: Long, end: Long): Flow<List<TransactionEntity>>

    /**
     * Transactions that moved money in or out of an account after [after],
     * oldest first. Used to bring the latest balance snapshot up to date; a
     * transaction at exactly [after] counts as already included in it.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE (account_id = :accountId OR to_account_id = :accountId)
          AND occurred_at > :after
        ORDER BY occurred_at, id
        """,
    )
    fun observeForAccountAfter(accountId: Long, after: Long): Flow<List<TransactionEntity>>

    /**
     * Card spending from [since] (inclusive), oldest first, both single payments
     * and installment purchases. Used for spending since the latest statement.
     */
    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'EXPENSE' AND card_id = :cardId AND occurred_at >= :since
        ORDER BY occurred_at, id
        """,
    )
    fun observeCardSpendingSince(cardId: Long, since: Long): Flow<List<TransactionEntity>>

    /** Every installment purchase on a card, oldest first, to build installment plans. */
    @Query(
        """
        SELECT * FROM transactions
        WHERE type = 'EXPENSE' AND card_id = :cardId AND installment_count IS NOT NULL
        ORDER BY occurred_at, id
        """,
    )
    fun observeInstallmentPurchases(cardId: Long): Flow<List<TransactionEntity>>

    /** The total paid toward a statement so far, in minor units. */
    @Query(
        """
        SELECT COALESCE(SUM(amount_minor), 0) FROM transactions
        WHERE type = 'CARD_PAYMENT' AND card_statement_id = :statementId
        """,
    )
    fun observePaidToward(statementId: Long): Flow<Long>
}
