package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    /** Inserts a new account and returns its generated id. */
    @Insert
    suspend fun insert(account: AccountEntity): Long

    @Update
    suspend fun update(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun findById(id: Long): AccountEntity?

    /** Active accounts in creation order. Emits again whenever they change. */
    @Query("SELECT * FROM accounts WHERE archived_at IS NULL ORDER BY created_at, id")
    fun observeActive(): Flow<List<AccountEntity>>

    /** Hides an account without deleting it, so its history keeps its meaning. */
    @Query("UPDATE accounts SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)
}
