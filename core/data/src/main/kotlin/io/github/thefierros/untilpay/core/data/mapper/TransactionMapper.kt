package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import io.github.thefierros.untilpay.core.domain.InstallmentPlan
import io.github.thefierros.untilpay.core.model.Money
import java.time.Instant
import java.time.ZoneId
import java.util.Currency

/** How this transaction changes the balance of account [accountId], in minor units. */
internal fun TransactionEntity.effectOn(accountId: Long): Long = fromRow("transaction $id") {
    require(amountMinor > 0) { "amount must be positive, got $amountMinor" }
    when (type) {
        "INCOME" -> amountMinor
        "EXPENSE", "CARD_PAYMENT" -> -amountMinor
        "TRANSFER" -> if (toAccountId == accountId) amountMinor else -amountMinor
        else -> throw IllegalArgumentException("unknown transaction type '$type'")
    }
}

/**
 * The installment plan of this card purchase. Every statement that closed on
 * or after the day of the purchase, in [zone], has billed one installment.
 */
internal fun TransactionEntity.asInstallmentPlan(
    currency: Currency,
    statements: List<CardStatementEntity>,
    zone: ZoneId,
): InstallmentPlan = fromRow("transaction $id") {
    val count = requireNotNull(installmentCount) { "an installment purchase needs a count" }
    val purchaseDay = Instant.ofEpochMilli(occurredAt).atZone(zone).toLocalDate().toEpochDay()
    val billed = statements.count { it.closingDate >= purchaseDay }
    InstallmentPlan(
        total = Money(amountMinor, currency),
        count = count,
        billedCount = billed.coerceAtMost(count),
    )
}
