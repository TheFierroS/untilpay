package io.github.thefierros.untilpay.core.model

import java.time.LocalDate

/**
 * A cost paid on the same day every month, such as a dormitory fee or a phone
 * bill. Its payment days follow the same calendar rule as monthly incomes.
 */
data class FixedCost(
    val id: FixedCostId,
    val name: String,
    val amount: Money,
    /** Day of the month it is paid, from 1 to 31; short months use their last day. */
    val dayOfMonth: Int,
) {
    init {
        require(name.isNotBlank()) { "Fixed cost name must not be blank" }
        require(amount.isPositive) { "amount must be positive, got $amount" }
        require(dayOfMonth in 1..31) { "dayOfMonth must be between 1 and 31, got $dayOfMonth" }
    }

    /** Every payment day from [from] (inclusive) up to [until] (exclusive). */
    fun dueDatesBetween(from: LocalDate, until: LocalDate): List<LocalDate> {
        val schedule = IncomeSchedule.Monthly(dayOfMonth)
        return generateSequence(schedule.nextAfter(from.minusDays(1))) { schedule.nextAfter(it) }
            .takeWhile { it.isBefore(until) }
            .toList()
    }
}
