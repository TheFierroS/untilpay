package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.AccountType
import java.util.Currency

internal fun AccountEntity.asModel(): Account = fromRow("account $id") {
    Account(
        id = AccountId(id),
        name = name,
        type = AccountType.valueOf(type),
        currency = Currency.getInstance(currencyCode),
        includeInBudget = includeInBudget,
    )
}
