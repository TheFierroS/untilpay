package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.IncomeSource
import io.github.thefierros.untilpay.core.model.IncomeSourceId
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomIncomeRepositoryTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")
    private val now = Instant.parse("2026-10-18T09:00:00Z")

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomIncomeRepository

    private var bank = AccountId(0)
    private var euroAccount = AccountId(0)

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomIncomeRepository(
            incomeSourceDao = database.incomeSourceDao(),
            accountDao = database.accountDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
        runBlocking {
            bank = AccountId(insertAccount("Ziraat", currencyCode = "TRY"))
            euroAccount = AccountId(insertAccount("Euro savings", currencyCode = "EUR"))
        }
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun insertAccount(name: String, currencyCode: String): Long =
        database.accountDao().insert(
            AccountEntity(
                name = name,
                type = "BANK",
                currencyCode = currencyCode,
                includeInBudget = true,
                createdAt = 1_000,
            ),
        )

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    private suspend fun create(
        expectedAmount: Money = tl(5_000_00),
        accountId: AccountId = bank,
    ): IncomeSourceId = repository.createIncomeSource(
        name = "Allowance",
        expectedAmount = expectedAmount,
        schedule = IncomeSchedule.Monthly(15),
        accountId = accountId,
    )

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        runCatching { block() }.exceptionOrNull()

    @Test
    fun `a created income source is listed`() = runTest {
        val id = create()

        assertEquals(
            listOf(IncomeSource(id, "Allowance", tl(5_000_00), IncomeSchedule.Monthly(15), bank)),
            repository.observeIncomeSources().first(),
        )
    }

    @Test
    fun `an amount in another currency than its account is rejected`() = runTest {
        val error = failureOf { create(expectedAmount = Money(100_00, euro)) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<IncomeSource>(), repository.observeIncomeSources().first())
    }

    @Test
    fun `income cannot be paid into an archived account`() = runTest {
        database.accountDao().archive(bank.value, archivedAt = 2_000)

        val error = failureOf { create() }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<IncomeSource>(), repository.observeIncomeSources().first())
    }

    @Test
    fun `an update can change the schedule but not break the currency`() = runTest {
        val id = create()
        val tutoring = IncomeSource(
            id = id,
            name = "Tutoring",
            expectedAmount = tl(800_00),
            schedule = IncomeSchedule.Weekly(DayOfWeek.FRIDAY),
            accountId = bank,
        )

        repository.updateIncomeSource(tutoring)
        val error =
            failureOf { repository.updateIncomeSource(tutoring.copy(accountId = euroAccount)) }

        assertEquals(listOf(tutoring), repository.observeIncomeSources().first())
        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
    }

    @Test
    fun `an archived income source is no longer listed but is kept`() = runTest {
        val id = create()

        repository.archiveIncomeSource(id)

        assertEquals(emptyList<IncomeSource>(), repository.observeIncomeSources().first())
        assertEquals(now.toEpochMilli(), database.incomeSourceDao().findById(id.value)?.archivedAt)
    }
}
