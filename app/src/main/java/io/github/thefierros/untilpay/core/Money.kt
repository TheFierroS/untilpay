package io.github.thefierros.untilpay.core

/**
 * An amount of money in kuruş (1/100 of a lira).
 *
 * Stored as a whole number so sums never pick up floating-point errors:
 * in floating point, 0.1 + 0.2 is not exactly 0.3, which is unacceptable for money.
 * Negative amounts are allowed; they represent shortfalls, such as spending
 * more than the cash on hand.
 */
@JvmInline
value class Money(val kurus: Long) : Comparable<Money> {

    operator fun plus(other: Money) = Money(kurus + other.kurus)

    operator fun minus(other: Money) = Money(kurus - other.kurus)

    /**
     * Splits the amount into [parts] equal shares, rounding down to the nearest kuruş.
     *
     * @throws IllegalArgumentException if [parts] is zero or negative.
     */
    operator fun div(parts: Long): Money {
        require(parts > 0) { "Cannot split money into $parts parts" }
        return Money(Math.floorDiv(kurus, parts))
    }

    override fun compareTo(other: Money) = kurus.compareTo(other.kurus)

    companion object {
        val ZERO = Money(0)

        fun lira(amount: Long) = Money(amount * 100)
    }
}
