package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A spending category, such as food or transport.
 *
 * Default categories are added by the data layer in the user's language; the
 * database only stores what it is given.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Creation time in epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    /** Archive time in epoch milliseconds, or null while the category is active. */
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,
)
