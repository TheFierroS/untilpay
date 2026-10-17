package io.github.thefierros.untilpay.core.domain

import io.github.thefierros.untilpay.core.model.Money

/**
 * A card purchase split into [count] monthly installments, each billed on its
 * own statement.
 *
 * Installment amounts are an estimate of what the bank bills: the total is
 * split evenly and the minor units that don't divide evenly go to the first
 * installment, so the installments always add up to exactly [total]. The
 * bank's statement remains the source of truth.
 */
data class InstallmentPlan(
    /** The full purchase amount, including any installment interest. */
    val total: Money,
    val count: Int,
    /** How many installments have already appeared on a statement. */
    val billedCount: Int,
) {
    init {
        require(total.isPositive) { "total must be positive, got $total" }
        require(count in 1..MAX_INSTALLMENTS) {
            "count must be between 1 and $MAX_INSTALLMENTS, got $count"
        }
        require(billedCount in 0..count) {
            "billedCount must be between 0 and $count, got $billedCount"
        }
    }

    /** The installment billed on the next statement, or null if all are billed. */
    val nextInstallment: Money?
        get() = if (billedCount < count) installment(billedCount + 1) else null

    /** Every installment that has not appeared on a statement yet. */
    val unbilled: Money
        get() = ((billedCount + 1)..count)
            .map { installment(it) }
            .fold(Money.zero(total.currency)) { sum, amount -> sum + amount }

    /** The amount of installment [number], counting from 1. */
    fun installment(number: Int): Money {
        require(number in 1..count) { "number must be between 1 and $count, got $number" }
        val evenShare = total / count.toLong()
        if (number > 1) return evenShare
        val leftover = total.minorUnits - evenShare.minorUnits * count
        return Money(evenShare.minorUnits + leftover, total.currency)
    }

    companion object {
        /**
         * A sanity limit against typos such as 600 instead of 6, not a legal
         * limit: installment caps differ by country and change over time.
         */
        const val MAX_INSTALLMENTS = 60
    }
}
