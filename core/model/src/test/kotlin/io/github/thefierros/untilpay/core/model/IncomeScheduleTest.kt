package io.github.thefierros.untilpay.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IncomeScheduleTest {

    private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    @Test
    fun `monthly income later this month arrives this month`() {
        val schedule = IncomeSchedule.Monthly(dayOfMonth = 15)
        assertEquals(date(2026, 10, 15), schedule.nextAfter(date(2026, 10, 10)))
    }

    @Test
    fun `monthly income on or before today arrives next month`() {
        val schedule = IncomeSchedule.Monthly(dayOfMonth = 15)
        assertEquals(date(2026, 11, 15), schedule.nextAfter(date(2026, 10, 15)))
        assertEquals(date(2026, 11, 15), schedule.nextAfter(date(2026, 10, 20)))
    }

    @Test
    fun `monthly income moves into the next year after December`() {
        val schedule = IncomeSchedule.Monthly(dayOfMonth = 1)
        assertEquals(date(2027, 1, 1), schedule.nextAfter(date(2026, 12, 5)))
    }

    @Test
    fun `monthly income falls on the last day of short months`() {
        val schedule = IncomeSchedule.Monthly(dayOfMonth = 31)
        assertEquals(date(2027, 2, 28), schedule.nextAfter(date(2027, 1, 31)))
        assertEquals(date(2028, 2, 29), schedule.nextAfter(date(2028, 1, 31)))
        assertEquals(date(2026, 11, 30), schedule.nextAfter(date(2026, 10, 31)))
    }

    @Test
    fun `the shifted day of a short month counts as the income day`() {
        val schedule = IncomeSchedule.Monthly(dayOfMonth = 31)
        assertEquals(date(2027, 3, 31), schedule.nextAfter(date(2027, 2, 28)))
    }

    @Test
    fun `days of month outside 1 to 31 are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            IncomeSchedule.Monthly(dayOfMonth = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            IncomeSchedule.Monthly(dayOfMonth = 32)
        }
    }

    @Test
    fun `weekly income arrives on the next matching weekday`() {
        // 16 October 2026 is a Friday.
        val friday = date(2026, 10, 16)
        assertEquals(date(2026, 10, 19), IncomeSchedule.Weekly(DayOfWeek.MONDAY).nextAfter(friday))
        assertEquals(date(2026, 10, 23), IncomeSchedule.Weekly(DayOfWeek.FRIDAY).nextAfter(friday))
    }

    @Test
    fun `a one-off income arrives once`() {
        val schedule = IncomeSchedule.Once(date(2026, 11, 1))
        assertEquals(date(2026, 11, 1), schedule.nextAfter(date(2026, 10, 16)))
    }

    @Test
    fun `a one-off income on or before today never arrives again`() {
        val schedule = IncomeSchedule.Once(date(2026, 11, 1))
        assertNull(schedule.nextAfter(date(2026, 11, 1)))
        assertNull(schedule.nextAfter(date(2026, 11, 5)))
    }
}
