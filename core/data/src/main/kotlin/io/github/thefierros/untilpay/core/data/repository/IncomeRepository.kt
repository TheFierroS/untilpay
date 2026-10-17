package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.IncomeSource
import io.github.thefierros.untilpay.core.model.IncomeSourceId
import io.github.thefierros.untilpay.core.model.Money
import kotlinx.coroutines.flow.Flow

/** Creates and changes the user's recurring incomes. */
interface IncomeRepository {

    /** Active income sources in the order they were created. */
    fun observeIncomeSources(): Flow<List<IncomeSource>>

    /**
     * Creates an income source and returns its id. It must be paid into an
     * active account, in that account's currency.
     */
    suspend fun createIncomeSource(
        name: String,
        expectedAmount: Money,
        schedule: IncomeSchedule,
        accountId: AccountId,
    ): IncomeSourceId

    /** Saves a changed income source. It may move to another active account in its currency. */
    suspend fun updateIncomeSource(incomeSource: IncomeSource)

    /** Stops an income from counting. It is kept. */
    suspend fun archiveIncomeSource(id: IncomeSourceId)
}
