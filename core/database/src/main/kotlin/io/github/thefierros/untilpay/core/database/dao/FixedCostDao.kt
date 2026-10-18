package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.FixedCostEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FixedCostDao {

    /** Inserts a new fixed cost and returns its generated id. */
    @Insert
    suspend fun insert(fixedCost: FixedCostEntity): Long

    @Update
    suspend fun update(fixedCost: FixedCostEntity)

    @Query("SELECT * FROM fixed_costs WHERE id = :id")
    suspend fun findById(id: Long): FixedCostEntity?

    /** Active fixed costs in the order they are paid during a month. */
    @Query("SELECT * FROM fixed_costs WHERE archived_at IS NULL ORDER BY day_of_month, id")
    fun observeActive(): Flow<List<FixedCostEntity>>

    /** Stops a cost from counting without deleting it. */
    @Query("UPDATE fixed_costs SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)
}
