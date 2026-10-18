package io.github.thefierros.untilpay.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.BalanceSnapshotDao
import io.github.thefierros.untilpay.core.database.dao.CardStatementDao
import io.github.thefierros.untilpay.core.database.dao.CategoryDao
import io.github.thefierros.untilpay.core.database.dao.CreditCardDao
import io.github.thefierros.untilpay.core.database.dao.FixedCostDao
import io.github.thefierros.untilpay.core.database.dao.IncomeSourceDao
import io.github.thefierros.untilpay.core.database.dao.TransactionDao
import javax.inject.Singleton

/**
 * Tells Hilt how to build the database and hand out its DAOs.
 *
 * There is deliberately no destructive migration fallback: a schema change
 * must never silently erase the user's money records.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    private const val DATABASE_NAME = "untilpay.db"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UntilpayDatabase =
        Room.databaseBuilder(context, UntilpayDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideAccountDao(database: UntilpayDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideBalanceSnapshotDao(database: UntilpayDatabase): BalanceSnapshotDao =
        database.balanceSnapshotDao()

    @Provides
    fun provideCreditCardDao(database: UntilpayDatabase): CreditCardDao = database.creditCardDao()

    @Provides
    fun provideCardStatementDao(database: UntilpayDatabase): CardStatementDao =
        database.cardStatementDao()

    @Provides
    fun provideIncomeSourceDao(database: UntilpayDatabase): IncomeSourceDao =
        database.incomeSourceDao()

    @Provides
    fun provideCategoryDao(database: UntilpayDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: UntilpayDatabase): TransactionDao =
        database.transactionDao()

    @Provides
    fun provideFixedCostDao(database: UntilpayDatabase): FixedCostDao = database.fixedCostDao()
}
