package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.domain.BudgetInput
import kotlinx.coroutines.flow.Flow

/**
 * Reads the user's money as the budget engine needs it, together with the
 * budget settings and fixed costs.
 */
interface BudgetRepository {

    /** Everything the budget engine needs for today. */
    suspend fun loadInput(): BudgetInput

    /** The same input, read again after every change to the user's money or settings. */
    fun observeInput(): Flow<BudgetInput>
}
