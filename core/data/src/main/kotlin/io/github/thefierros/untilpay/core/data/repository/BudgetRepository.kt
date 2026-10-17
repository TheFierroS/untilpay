package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.domain.BudgetInput
import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency

/** Reads the user's money as the budget engine needs it. */
interface BudgetRepository {

    /**
     * Everything the budget engine needs for today.
     *
     * The budget [currency], [fixedCosts] and [protectedAmount] are passed in
     * until they are stored as user preferences.
     */
    suspend fun loadInput(
        currency: Currency,
        fixedCosts: Money,
        protectedAmount: Money,
    ): BudgetInput
}
