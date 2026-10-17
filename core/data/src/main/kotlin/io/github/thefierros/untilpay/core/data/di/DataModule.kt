package io.github.thefierros.untilpay.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.thefierros.untilpay.core.data.repository.AccountRepository
import io.github.thefierros.untilpay.core.data.repository.BudgetRepository
import io.github.thefierros.untilpay.core.data.repository.CardRepository
import io.github.thefierros.untilpay.core.data.repository.RoomAccountRepository
import io.github.thefierros.untilpay.core.data.repository.RoomBudgetRepository
import io.github.thefierros.untilpay.core.data.repository.RoomCardRepository
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindBudgetRepository(repository: RoomBudgetRepository): BudgetRepository

    companion object {

        /** The device clock, in the device's current time zone. */
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }

    @Binds
    abstract fun bindAccountRepository(repository: RoomAccountRepository): AccountRepository

    @Binds
    abstract fun bindCardRepository(repository: RoomCardRepository): CardRepository
}
