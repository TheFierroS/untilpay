package io.github.thefierros.untilpay.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.BalanceSnapshotDao
import io.github.thefierros.untilpay.core.database.dao.CardStatementDao
import io.github.thefierros.untilpay.core.database.dao.CategoryDao
import io.github.thefierros.untilpay.core.database.dao.CreditCardDao
import io.github.thefierros.untilpay.core.database.dao.FixedCostDao
import io.github.thefierros.untilpay.core.database.dao.IncomeSourceDao
import io.github.thefierros.untilpay.core.database.dao.TransactionDao
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity
import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.database.model.FixedCostEntity
import io.github.thefierros.untilpay.core.database.model.IncomeSourceEntity
import io.github.thefierros.untilpay.core.database.model.TransactionEntity

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
        CreditCardEntity::class,
        CardStatementEntity::class,
        IncomeSourceEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        FixedCostEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class UntilpayDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao

    abstract fun balanceSnapshotDao(): BalanceSnapshotDao

    abstract fun creditCardDao(): CreditCardDao

    abstract fun cardStatementDao(): CardStatementDao

    abstract fun incomeSourceDao(): IncomeSourceDao

    abstract fun categoryDao(): CategoryDao

    abstract fun transactionDao(): TransactionDao

    abstract fun fixedCostDao(): FixedCostDao
}
