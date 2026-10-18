package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.FixedCost
import io.github.thefierros.untilpay.core.model.FixedCostId
import io.github.thefierros.untilpay.core.model.Money
import kotlinx.coroutines.flow.Flow

/** Creates and changes the costs paid on the same day every month. */
interface FixedCostRepository {

    /** Active fixed costs in the order they are paid during a month. */
    fun observeFixedCosts(): Flow<List<FixedCost>>

    /** Creates a fixed cost and returns its id. */
    suspend fun createFixedCost(name: String, amount: Money, dayOfMonth: Int): FixedCostId

    /** Saves a changed name, amount or payment day. */
    suspend fun updateFixedCost(fixedCost: FixedCost)

    /** Stops a cost from counting. It is kept. */
    suspend fun archiveFixedCost(id: FixedCostId)
}
