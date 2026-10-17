package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardStatementDao {

    /** Inserts a new statement and returns its generated id. */
    @Insert
    suspend fun insert(statement: CardStatementEntity): Long

    /** Saves a corrected statement or a changed planned payment. */
    @Update
    suspend fun update(statement: CardStatementEntity)

    @Query("SELECT * FROM card_statements WHERE id = :id")
    suspend fun findById(id: Long): CardStatementEntity?

    /** The latest statement of one card, or null if it has none. */
    @Query(
        """
        SELECT * FROM card_statements
        WHERE card_id = :cardId
        ORDER BY closing_date DESC
        LIMIT 1
        """,
    )
    fun observeLatest(cardId: Long): Flow<CardStatementEntity?>

    /** The latest statement of every card that has one, ordered by card. */
    @Query(
        """
        SELECT * FROM card_statements AS statement
        WHERE statement.closing_date = (
            SELECT MAX(latest.closing_date) FROM card_statements AS latest
            WHERE latest.card_id = statement.card_id
        )
        ORDER BY statement.card_id
        """,
    )
    fun observeLatestPerCard(): Flow<List<CardStatementEntity>>
}
