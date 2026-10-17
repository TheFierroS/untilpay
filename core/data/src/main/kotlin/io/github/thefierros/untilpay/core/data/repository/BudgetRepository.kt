package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.domain.BudgetInput
import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency
import kotlinx.coroutines.flow.Flow

/**
 * Reads the user's money as the budget engine needs it.
 *
 * The budget currency, fixed costs and protected amount are passed in until
 * they are stored as user preferences.
 */
interface BudgetRepository {

    /** Everything the budget engine needs for today. */
    suspend fun loadInput(
        currency: Currency,
        fixedCosts: Money,
        protectedAmount: Money,
    ): BudgetInput

    /** The same input, read again after every change to the user's money. */
    fun observeInput(
        currency: Currency,
        fixedCosts: Money,
        protectedAmount: Money,
    ): Flow<BudgetInput>
}
