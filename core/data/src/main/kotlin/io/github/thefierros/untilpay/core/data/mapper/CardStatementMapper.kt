package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CardStatement
import io.github.thefierros.untilpay.core.model.Money
import io.github.thefierros.untilpay.core.model.StatementId
import java.time.LocalDate
import java.util.Currency

/** The statement in this row. Its amounts are in [currency], the currency of its card. */
internal fun CardStatementEntity.asModel(currency: Currency): CardStatement =
    fromRow("card statement $id") {
        CardStatement(
            id = StatementId(id),
            cardId = CardId(cardId),
            closingDate = LocalDate.ofEpochDay(closingDate),
            dueDate = LocalDate.ofEpochDay(dueDate),
            balance = Money(balanceMinor, currency),
            minimumPayment = Money(minimumPaymentMinor, currency),
            plannedPayment = Money(plannedPaymentMinor, currency),
        )
    }

internal fun CardStatement.asEntity() = CardStatementEntity(
    id = id.value,
    cardId = cardId.value,
    closingDate = closingDate.toEpochDay(),
    dueDate = dueDate.toEpochDay(),
    balanceMinor = balance.minorUnits,
    minimumPaymentMinor = minimumPayment.minorUnits,
    plannedPaymentMinor = plannedPayment.minorUnits,
)
