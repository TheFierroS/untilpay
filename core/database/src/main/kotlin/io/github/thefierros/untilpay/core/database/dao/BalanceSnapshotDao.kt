package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceSnapshotDao {

    /** Records a new balance and returns its generated id. */
    @Insert
    suspend fun insert(snapshot: BalanceSnapshotEntity): Long

    /** The latest snapshot of one account, or null if it has none. */
    @Query(
        """
        SELECT * FROM balance_snapshots
        WHERE account_id = :accountId
        ORDER BY recorded_at DESC, id DESC
        LIMIT 1
        """,
    )
    fun observeLatest(accountId: Long): Flow<BalanceSnapshotEntity?>

    /** The latest snapshot of every account that has one, ordered by account. */
    @Query(
        """
        SELECT * FROM balance_snapshots AS snapshot
        WHERE snapshot.id = (
            SELECT latest.id FROM balance_snapshots AS latest
            WHERE latest.account_id = snapshot.account_id
            ORDER BY latest.recorded_at DESC, latest.id DESC
            LIMIT 1
        )
        ORDER BY snapshot.account_id
        """,
    )
    fun observeLatestPerAccount(): Flow<List<BalanceSnapshotEntity>>
}
