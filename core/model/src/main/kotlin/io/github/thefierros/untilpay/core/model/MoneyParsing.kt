package io.github.thefierros.untilpay.core.model

import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Currency
import java.util.Locale

/**
 * Parses an amount typed by the user, following [locale]'s separators:
 * "1.500,50" in Turkish and "1,500.50" in English are both 1500.50.
 *
 * Returns null instead of guessing whenever the input is invalid or
 * ambiguous. In particular, "15.50" in Turkish is rejected rather than read
 * as 1550: a dot groups thousands there, so it must be followed by exactly
 * three digits. Asking the user to fix a typo is far better than silently
 * recording a hundred times the intended amount.
 *
 * Accepted: surrounding spaces, a leading minus sign, grouping separators
 * in valid positions (a plain space where the locale groups with a special
 * space), a trailing decimal separator while typing ("15,"), and at most as
 * many decimal places as [currency] has.
 *
 * Not supported: Indian-style grouping such as "1,00,000". Typing amounts
 * without grouping works in every locale.
 */
fun Money.Companion.parseOrNull(text: String, currency: Currency, locale: Locale): Money? {
    val symbols = DecimalFormatSymbols.getInstance(locale)
    val trimmed = text.trim()
    val negative = trimmed.startsWith('-') || trimmed.startsWith(symbols.minusSign)
    val unsigned = if (negative) trimmed.drop(1) else trimmed

    val parts = unsigned.split(symbols.decimalSeparator)
    if (parts.size > 2) return null

    val integerDigits = integerDigitsOrNull(parts[0], symbols.groupingSeparator) ?: return null
    val fractionDigits = parts.getOrElse(1) { "" }
    if (!fractionDigits.all { it in '0'..'9' }) return null
    if (integerDigits.isEmpty() && fractionDigits.isEmpty()) return null
    if (fractionDigits.length > currency.defaultFractionDigits) return null

    val amount = BigDecimal("${integerDigits.ifEmpty { "0" }}.${fractionDigits.ifEmpty { "0" }}")
    return try {
        Money.of(if (negative) amount.negate() else amount, currency)
    } catch (_: ArithmeticException) {
        null
    }
}

/**
 * Returns the digits of the integer part with grouping separators removed,
 * or null if it contains anything else or the separators are misplaced.
 * Without separators any number of digits is fine; with them, the first
 * group has one to three digits and every following group exactly three.
 */
private fun integerDigitsOrNull(part: String, groupingSeparator: Char): String? {
    val separators = if (groupingSeparator.isWhitespace()) {
        charArrayOf(groupingSeparator, ' ', '\u00A0', '\u202F')
    } else {
        charArrayOf(groupingSeparator)
    }
    val groups = part.split(*separators)
    if (groups.any { group -> !group.all { it in '0'..'9' } }) return null
    if (groups.size == 1) return groups.single()
    if (groups.first().length !in 1..3) return null
    if (groups.drop(1).any { it.length != 3 }) return null
    return groups.joinToString("")
}
