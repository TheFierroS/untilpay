package io.github.thefierros.untilpay.core

/**
 * An amount of money in kuruş (1/100 of a lira).
 *
 * Stored as a whole number so sums never pick up floating-point errors:
 * in floating point, 0.1 + 0.2 is not exactly 0.3, which is unacceptable for money.
 */
@JvmInline
value class Money(val kurus: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(kurus + other.kurus)

    operator fun minus(other: Money) = Money(kurus - other.kurus)

    /** Splits the amount into equal parts, rounding down to the nearest kuruş. */
    operator fun div(parts: Long) = Money(Math.floorDiv(kurus, parts))

    override fun compareTo(other: Money) = kurus.compareTo(other.kurus)

    companion object {
        val ZERO = Money(0)

        fun lira(amount: Long) = Money(amount * 100)
    }
}