package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.database.dao.FixedCostDao
import io.github.thefierros.untilpay.core.database.model.FixedCostEntity
import io.github.thefierros.untilpay.core.model.FixedCost
import io.github.thefierros.untilpay.core.model.FixedCostId
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomFixedCostRepository @Inject constructor(
    private val fixedCostDao: FixedCostDao,
    private val clock: Clock,
) : FixedCostRepository {

    override fun observeFixedCosts(): Flow<List<FixedCost>> =
        fixedCostDao.observeActive().map { rows -> rows.map { it.asModel() } }

    override suspend fun createFixedCost(
        name: String,
        amount: Money,
        dayOfMonth: Int,
    ): FixedCostId {
        // Building the model first applies its rules, such as a day within a month.
        val draft = FixedCost(FixedCostId(0), name, amount, dayOfMonth)
        return FixedCostId(
            fixedCostDao.insert(
                FixedCostEntity(
                    name = draft.name,
                    amountMinor = draft.amount.minorUnits,
                    currencyCode = draft.amount.currency.currencyCode,
                    dayOfMonth = draft.dayOfMonth,
                    createdAt = clock.millis(),
                ),
            ),
        )
    }

    override suspend fun updateFixedCost(fixedCost: FixedCost) {
        val row = existing(fixedCost.id)
        fixedCostDao.update(
            row.copy(
                name = fixedCost.name,
                amountMinor = fixedCost.amount.minorUnits,
                currencyCode = fixedCost.amount.currency.currencyCode,
                dayOfMonth = fixedCost.dayOfMonth,
            ),
        )
    }

    override suspend fun archiveFixedCost(id: FixedCostId) {
        fixedCostDao.archive(existing(id).id, archivedAt = clock.millis())
    }

    private suspend fun existing(id: FixedCostId): FixedCostEntity =
        requireNotNull(fixedCostDao.findById(id.value)) { "There is no fixed cost ${id.value}" }
}
