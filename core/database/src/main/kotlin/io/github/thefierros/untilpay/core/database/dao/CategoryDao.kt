package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /** Inserts a new category and returns its generated id. */
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun findById(id: Long): CategoryEntity?

    /** Active categories in creation order. Emits again whenever they change. */
    @Query("SELECT * FROM categories WHERE archived_at IS NULL ORDER BY created_at, id")
    fun observeActive(): Flow<List<CategoryEntity>>

    /** Hides a category without deleting it, so past spending keeps its category. */
    @Query("UPDATE categories SET archived_at = :archivedAt WHERE id = :id")
    suspend fun archive(id: Long, archivedAt: Long)
}
