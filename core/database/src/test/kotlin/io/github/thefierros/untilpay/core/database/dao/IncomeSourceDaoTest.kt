package io.github.thefierros.untilpay.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.IncomeSourceEntity
import java.time.DayOfWeek
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IncomeSourceDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: IncomeSourceDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.incomeSourceDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun insertAccount(): Long = database.accountDao().insert(
        AccountEntity(
            name = "Ziraat",
            type = "BANK",
            currencyCode = "TRY",
            includeInBudget = true,
            createdAt = 1_000,
        ),
    )

    private fun monthly(accountId: Long, name: String, createdAt: Long) = IncomeSourceEntity(
        name = name,
        expectedAmountMinor = 5_000_00,
        accountId = accountId,
        scheduleType = "MONTHLY",
        scheduleDayOfMonth = 15,
        createdAt = createdAt,
    )

    @Test
    fun `an inserted income source can be read back with its generated id`() = runTest {
        val account = insertAccount()
        val id = dao.insert(monthly(account, "Allowance", createdAt = 1_000))

        assertEquals(
            monthly(account, "Allowance", createdAt = 1_000).copy(id = id),
            dao.findById(id),
        )
    }

    @Test
    fun `each kind of schedule keeps only its own column`() = runTest {
        val account = insertAccount()
        val weekly = IncomeSourceEntity(
            name = "Tutoring",
            expectedAmountMinor = 800_00,
            accountId = account,
            scheduleType = "WEEKLY",
            scheduleDayOfWeek = DayOfWeek.FRIDAY.value,
            createdAt = 1_000,
        )
        val once = IncomeSourceEntity(
            name = "Birthday",
            expectedAmountMinor = 1_000_00,
            accountId = account,
            scheduleType = "ONCE",
            scheduleDate = LocalDate.of(2026, 11, 3).toEpochDay(),
            createdAt = 2_000,
        )

        val weeklyId = dao.insert(weekly)
        val onceId = dao.insert(once)

        assertEquals(weekly.copy(id = weeklyId), dao.findById(weeklyId))
        assertEquals(once.copy(id = onceId), dao.findById(onceId))
    }

    @Test
    fun `active income sources are listed in creation order without archived ones`() = runTest {
        val account = insertAccount()
        dao.insert(monthly(account, "Scholarship", createdAt = 2_000))
        val ended = dao.insert(monthly(account, "Summer job", createdAt = 1_500))
        dao.insert(monthly(account, "Allowance", createdAt = 1_000))

        dao.archive(ended, archivedAt = 3_000)

        val names = dao.observeActive().first().map { it.name }
        assertEquals(listOf("Allowance", "Scholarship"), names)
    }

    @Test
    fun `a changed expected amount is saved`() = runTest {
        val account = insertAccount()
        val id = dao.insert(monthly(account, "Allowance", createdAt = 1_000))
        val original = checkNotNull(dao.findById(id))

        dao.update(original.copy(expectedAmountMinor = 6_000_00))

        assertEquals(6_000_00L, dao.findById(id)?.expectedAmountMinor)
    }

    @Test
    fun `an income source for a missing account is rejected`() = runTest {
        val error = runCatching {
            dao.insert(monthly(accountId = 42, name = "Allowance", createdAt = 1_000))
        }.exceptionOrNull()

        assertTrue(
            "Expected a foreign key violation but got $error",
            error is SQLiteConstraintException,
        )
    }
}
