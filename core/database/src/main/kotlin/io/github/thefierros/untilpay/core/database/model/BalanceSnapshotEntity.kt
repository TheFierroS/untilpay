package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The balance of an account at a moment in time.
 *
 * Snapshots are never changed: a correction is a newer snapshot. The current
 * balance of an account is its latest snapshot.
 */
@Entity(
    tableName = "balance_snapshots",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["account_id", "recorded_at"])],
)
data class BalanceSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    /** Balance in the account's currency, in minor units. May be negative. */
    @ColumnInfo(name = "balance_minor")
    val balanceMinor: Long,
    /** When the balance was recorded, in epoch milliseconds. */
    @ColumnInfo(name = "recorded_at")
    val recordedAt: Long,
)
