package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {

    /** Inserts a new card and returns its generated id. */
    @Insert
    suspend fun insert(card: CreditCardEntity): Long

    @Update
    suspend fun update(card: CreditCardEntity)

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun findById(id: Long): CreditCardEntity?

    /** Active cards in creation order. Emits again whenever they change. */
    @Query("SELECT * FROM credit_cards WHERE archived_at IS NULL ORDER BY created_at, id")
    fun observeActive(): Flow<List<CreditCardEntity>>

    /** Hides a card without deleting it, so its statements keep their meaning. */
    @Query("UPDATE credit_cards SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)
}
