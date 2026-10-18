package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.model.FixedCost
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Currency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomFixedCostRepositoryTest {

    private val lira = Currency.getInstance("TRY")
    private val now = Instant.parse("2026-10-19T09:00:00Z")

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomFixedCostRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomFixedCostRepository(
            fixedCostDao = database.fixedCostDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        runCatching { block() }.exceptionOrNull()

    @Test
    fun `created fixed costs are listed by payment day`() = runTest {
        val phone = repository.createFixedCost("Phone", tl(300_00), dayOfMonth = 15)
        val dormitory = repository.createFixedCost("Dormitory", tl(1_590_00), dayOfMonth = 1)

        assertEquals(
            listOf(
                FixedCost(dormitory, "Dormitory", tl(1_590_00), dayOfMonth = 1),
                FixedCost(phone, "Phone", tl(300_00), dayOfMonth = 15),
            ),
            repository.observeFixedCosts().first(),
        )
    }

    @Test
    fun `an invalid fixed cost is rejected before anything is saved`() = runTest {
        val error = failureOf { repository.createFixedCost("Phone", tl(300_00), dayOfMonth = 32) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<FixedCost>(), repository.observeFixedCosts().first())
    }

    @Test
    fun `a changed fixed cost is saved`() = runTest {
        val id = repository.createFixedCost("Phone", tl(300_00), dayOfMonth = 15)

        repository.updateFixedCost(FixedCost(id, "Phone", tl(350_00), dayOfMonth = 20))

        assertEquals(
            listOf(FixedCost(id, "Phone", tl(350_00), dayOfMonth = 20)),
            repository.observeFixedCosts().first(),
        )
    }

    @Test
    fun `an archived fixed cost is no longer listed but is kept`() = runTest {
        val id = repository.createFixedCost("Gym", tl(400_00), dayOfMonth = 5)

        repository.archiveFixedCost(id)

        assertEquals(emptyList<FixedCost>(), repository.observeFixedCosts().first())
        assertEquals(now.toEpochMilli(), database.fixedCostDao().findById(id.value)?.archivedAt)
    }
}
