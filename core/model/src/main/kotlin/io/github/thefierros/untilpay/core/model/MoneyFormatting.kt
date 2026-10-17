package io.github.thefierros.untilpay.core.model

import java.text.NumberFormat
import java.util.Locale

/**
 * Formats the amount for display following [locale]'s conventions for the
 * currency symbol, digit grouping and decimal separator: 150050 kuruş is
 * "₺1.500,50" in Turkish, while 150050 euro cents are "1.500,50 €" in German.
 *
 * Always shows exactly as many decimal places as the currency has, so 15 TRY
 * is shown as "₺15,00" and amounts line up in lists.
 */
fun Money.format(locale: Locale): String {
    val money = this
    val formatter = NumberFormat.getCurrencyInstance(locale).apply {
        currency = money.currency
        minimumFractionDigits = money.currency.defaultFractionDigits
        maximumFractionDigits = money.currency.defaultFractionDigits
    }
    return formatter.format(money.toBigDecimal())
}
