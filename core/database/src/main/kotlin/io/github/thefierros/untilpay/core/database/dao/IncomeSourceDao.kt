package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.IncomeSourceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeSourceDao {

    /** Inserts a new income source and returns its generated id. */
    @Insert
    suspend fun insert(incomeSource: IncomeSourceEntity): Long

    @Update
    suspend fun update(incomeSource: IncomeSourceEntity)

    @Query("SELECT * FROM income_sources WHERE id = :id")
    suspend fun findById(id: Long): IncomeSourceEntity?

    /** Active income sources in creation order. Emits again whenever they change. */
    @Query("SELECT * FROM income_sources WHERE archived_at IS NULL ORDER BY created_at, id")
    fun observeActive(): Flow<List<IncomeSourceEntity>>

    /** Stops an income from counting without deleting it. */
    @Query("UPDATE income_sources SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)
}
