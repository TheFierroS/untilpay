package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency

internal fun requireCurrency(amount: Money, expected: Currency, name: String) {
    require(amount.currency == expected) {
        "$name must be in ${expected.currencyCode}, got ${amount.currency.currencyCode}"
    }
}

internal fun requireNotNegative(amount: Money, name: String) {
    require(!amount.isNegative) { "$name must not be negative, got $amount" }
}
