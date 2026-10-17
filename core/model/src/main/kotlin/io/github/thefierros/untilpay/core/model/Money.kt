package io.github.thefierros.untilpay.core.model

import java.util.Currency

/**
 * An amount of money in a specific currency.
 *
 * The amount is stored as a whole number of the currency's minor units
 * (kuruş for TRY, cents for EUR, yen for JPY, fils for KWD), so sums never
 * pick up floating-point errors: in floating point, 0.1 + 0.2 is not exactly
 * 0.3, which is unacceptable for money.
 *
 * Negative amounts are allowed; they represent shortfalls, such as spending
 * more than the cash on hand. Amounts in different currencies are never
 * combined or compared: trying to do so throws instead of silently mixing them.
 */
data class Money(val minorUnits: Long, val currency: Currency) : Comparable<Money> {

    init {
        require(currency.defaultFractionDigits >= 0) {
            "${currency.currencyCode} has no fixed minor units and cannot be used as money"
        }
    }

    val isPositive: Boolean get() = minorUnits > 0

    val isNegative: Boolean get() = minorUnits < 0

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.addExact(minorUnits, other.minorUnits), currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(Math.subtractExact(minorUnits, other.minorUnits), currency)
    }

    /**
     * Splits the amount into [parts] equal shares, rounding down to the
     * nearest minor unit.
     *
     * @throws IllegalArgumentException if [parts] is zero or negative.
     */
    operator fun div(parts: Long): Money {
        require(parts > 0) { "Cannot split money into $parts parts" }
        return Money(Math.floorDiv(minorUnits, parts), currency)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return minorUnits.compareTo(other.minorUnits)
    }

    private fun requireSameCurrency(other: Money) {
        require(currency == other.currency) {
            "Cannot combine ${currency.currencyCode} with ${other.currency.currencyCode}"
        }
    }

    companion object {
        fun zero(currency: Currency) = Money(0, currency)
    }
}
