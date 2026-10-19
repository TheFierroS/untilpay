package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.BudgetSettings
import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency
import kotlinx.coroutines.flow.Flow

/** The user's budget settings. */
interface SettingsRepository {

    fun observeSettings(): Flow<BudgetSettings>

    /** Changes the budget currency; a protected amount in the old currency is cleared. */
    suspend fun setCurrency(currency: Currency)

    /** Saves the protected amount; it must be in the budget currency and not negative. */
    suspend fun setProtectedAmount(amount: Money)
}
