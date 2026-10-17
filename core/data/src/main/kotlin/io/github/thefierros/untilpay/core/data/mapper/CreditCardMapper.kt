package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CreditCard
import java.util.Currency

internal fun CreditCardEntity.asModel(): CreditCard = fromRow("credit card $id") {
    CreditCard(
        id = CardId(id),
        name = name,
        currency = Currency.getInstance(currencyCode),
        statementDay = statementDay,
        dueDay = dueDay,
    )
}
