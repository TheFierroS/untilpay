package io.github.thefierros.untilpay.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/** When a recurring income arrives. */
sealed interface IncomeSchedule {

    /**
     * The first date strictly after [day] on which the income arrives, or null
     * if it will never arrive again. An income arriving on [day] itself is
     * considered already received.
     */
    fun nextAfter(day: LocalDate): LocalDate?

    /**
     * Every month on [dayOfMonth]. In months that are too short, such as
     * February for day 31, the income arrives on the last day of the month.
     */
    data class Monthly(val dayOfMonth: Int) : IncomeSchedule {

        init {
            require(dayOfMonth in 1..31) { "dayOfMonth must be between 1 and 31, got $dayOfMonth" }
        }

        override fun nextAfter(day: LocalDate): LocalDate {
            val thisMonth = YearMonth.from(day)
            val occurrenceThisMonth = occurrenceIn(thisMonth)
            return if (occurrenceThisMonth.isAfter(day)) {
                occurrenceThisMonth
            } else {
                occurrenceIn(thisMonth.plusMonths(1))
            }
        }

        private fun occurrenceIn(month: YearMonth): LocalDate =
            month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))
    }

    /** Every week on [dayOfWeek]. */
    data class Weekly(val dayOfWeek: DayOfWeek) : IncomeSchedule {

        override fun nextAfter(day: LocalDate): LocalDate =
            day.with(TemporalAdjusters.next(dayOfWeek))
    }

    /** A single payment on [date], such as a one-off transfer. */
    data class Once(val date: LocalDate) : IncomeSchedule {

        override fun nextAfter(day: LocalDate): LocalDate? = date.takeIf { it.isAfter(day) }
    }
}
