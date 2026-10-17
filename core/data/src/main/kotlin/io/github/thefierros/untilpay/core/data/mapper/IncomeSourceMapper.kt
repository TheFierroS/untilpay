package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.IncomeSourceEntity
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.IncomeSource
import io.github.thefierros.untilpay.core.model.IncomeSourceId
import io.github.thefierros.untilpay.core.model.Money
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Currency

private const val MONTHLY = "MONTHLY"
private const val WEEKLY = "WEEKLY"
private const val ONCE = "ONCE"

/** How an [IncomeSchedule] is spread over the schedule columns of a row. */
internal data class ScheduleColumns(
    val type: String,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val date: Long? = null,
)

internal fun IncomeSchedule.toColumns(): ScheduleColumns = when (this) {
    is IncomeSchedule.Monthly -> ScheduleColumns(MONTHLY, dayOfMonth = dayOfMonth)
    is IncomeSchedule.Weekly -> ScheduleColumns(WEEKLY, dayOfWeek = dayOfWeek.value)
    is IncomeSchedule.Once -> ScheduleColumns(ONCE, date = date.toEpochDay())
}

/**
 * The income source in this row. Its amount is in [currency], the currency of
 * the account it is paid into, because the row does not repeat it.
 */
internal fun IncomeSourceEntity.asModel(currency: Currency): IncomeSource =
    fromRow("income source $id") {
        IncomeSource(
            id = IncomeSourceId(id),
            name = name,
            expectedAmount = Money(expectedAmountMinor, currency),
            schedule = schedule(),
            accountId = AccountId(accountId),
        )
    }

private fun IncomeSourceEntity.schedule(): IncomeSchedule {
    val schedule = when (scheduleType) {
        MONTHLY -> IncomeSchedule.Monthly(
            requireNotNull(scheduleDayOfMonth) { "a monthly schedule needs a day of the month" },
        )

        WEEKLY -> IncomeSchedule.Weekly(
            DayOfWeek.of(
                requireNotNull(scheduleDayOfWeek) { "a weekly schedule needs a day of the week" },
            ),
        )

        ONCE -> IncomeSchedule.Once(
            LocalDate.ofEpochDay(
                requireNotNull(scheduleDate) {
                    "a one-time schedule needs a date"
                },
            ),
        )

        else -> throw IllegalArgumentException("unknown schedule type '$scheduleType'")
    }
    val stored = ScheduleColumns(scheduleType, scheduleDayOfMonth, scheduleDayOfWeek, scheduleDate)
    require(schedule.toColumns() == stored) {
        "a $scheduleType schedule has columns of another kind of schedule filled"
    }
    return schedule
}
