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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class IncomeSourceMapperTest {

    private val lira = Currency.getInstance("TRY")

    private fun row(columns: ScheduleColumns) = IncomeSourceEntity(
        id = 3,
        name = "Allowance",
        expectedAmountMinor = 5_000_00,
        accountId = 1,
        scheduleType = columns.type,
        scheduleDayOfMonth = columns.dayOfMonth,
        scheduleDayOfWeek = columns.dayOfWeek,
        scheduleDate = columns.date,
        createdAt = 1_000,
    )

    @Test
    fun `a row becomes an income source in the given currency`() {
        val expected = IncomeSource(
            id = IncomeSourceId(3),
            name = "Allowance",
            expectedAmount = Money(5_000_00, lira),
            schedule = IncomeSchedule.Monthly(15),
            accountId = AccountId(1),
        )

        assertEquals(expected, row(IncomeSchedule.Monthly(15).toColumns()).asModel(lira))
    }

    @Test
    fun `every kind of schedule survives a round trip`() {
        val schedules = listOf(
            IncomeSchedule.Monthly(31),
            IncomeSchedule.Weekly(DayOfWeek.FRIDAY),
            IncomeSchedule.Once(LocalDate.of(2026, 11, 3)),
        )

        for (schedule in schedules) {
            assertEquals(schedule, row(schedule.toColumns()).asModel(lira).schedule)
        }
    }

    @Test
    fun `days of the week are stored as ISO numbers`() {
        assertEquals(1, IncomeSchedule.Weekly(DayOfWeek.MONDAY).toColumns().dayOfWeek)
        assertEquals(7, IncomeSchedule.Weekly(DayOfWeek.SUNDAY).toColumns().dayOfWeek)
    }

    @Test
    fun `a monthly schedule without a day fails and names the row`() {
        val error = assertThrows(IllegalStateException::class.java) {
            row(ScheduleColumns("MONTHLY")).asModel(lira)
        }

        assertTrue(error.message.orEmpty().contains("income source 3"))
    }

    @Test
    fun `a schedule with another kind's column filled fails`() {
        val mixed = ScheduleColumns("MONTHLY", dayOfMonth = 15, dayOfWeek = 5)

        assertThrows(IllegalStateException::class.java) { row(mixed).asModel(lira) }
    }

    @Test
    fun `an unknown schedule type fails`() {
        val yearly = ScheduleColumns("YEARLY", dayOfMonth = 1)

        assertThrows(IllegalStateException::class.java) { row(yearly).asModel(lira) }
    }

    @Test
    fun `an invalid day of the week fails`() {
        val invalid = ScheduleColumns("WEEKLY", dayOfWeek = 8)

        assertThrows(IllegalStateException::class.java) { row(invalid).asModel(lira) }
    }
}
