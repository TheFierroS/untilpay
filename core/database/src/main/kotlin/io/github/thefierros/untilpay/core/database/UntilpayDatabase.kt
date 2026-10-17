package io.github.thefierros.untilpay.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.BalanceSnapshotDao
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity

/**
 * The app's database.
 *
 * Stays at version 1 until the first release; after that, every schema change
 * needs a new version and a tested migration.
 */
@Database(
    entities = [
        AccountEntity::class,
        BalanceSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UntilpayDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun balanceSnapshotDao(): BalanceSnapshotDao
}
