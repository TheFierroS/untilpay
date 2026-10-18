package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.datastore.BudgetSettingsDataSource
import io.github.thefierros.untilpay.core.model.BudgetSettings
import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

internal class DataStoreSettingsRepository @Inject constructor(
    private val dataSource: BudgetSettingsDataSource,
) : SettingsRepository {

    override fun observeSettings(): Flow<BudgetSettings> = dataSource.settings

    override suspend fun setCurrency(currency: Currency) = dataSource.setCurrency(currency)

    override suspend fun setProtectedAmount(amount: Money) = dataSource.setProtectedAmount(amount)
}
