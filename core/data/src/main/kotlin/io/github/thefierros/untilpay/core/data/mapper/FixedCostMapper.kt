package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.FixedCostEntity
import io.github.thefierros.untilpay.core.model.FixedCost
import io.github.thefierros.untilpay.core.model.FixedCostId
import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency

internal fun FixedCostEntity.asModel(): FixedCost = fromRow("fixed cost $id") {
    FixedCost(
        id = FixedCostId(id),
        name = name,
        amount = Money(amountMinor, Currency.getInstance(currencyCode)),
        dayOfMonth = dayOfMonth,
    )
}
