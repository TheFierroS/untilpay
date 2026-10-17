package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CreditCard
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
class RoomCardRepositoryTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")
    private val now = Instant.parse("2026-10-18T09:00:00Z")

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomCardRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomCardRepository(
            creditCardDao = database.creditCardDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun create(
        name: String = "Bankkart",
        statementDay: Int = 15,
        dueDay: Int = 25,
    ): CardId = repository.createCard(name, lira, statementDay, dueDay)

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        runCatching { block() }.exceptionOrNull()

    @Test
    fun `a created card is listed`() = runTest {
        val id = create()

        assertEquals(
            listOf(CreditCard(id, "Bankkart", lira, statementDay = 15, dueDay = 25)),
            repository.observeCards().first(),
        )
    }

    @Test
    fun `cards are listed in creation order`() = runTest {
        create(name = "Bankkart")
        create(name = "Other")

        assertEquals(listOf("Bankkart", "Other"), repository.observeCards().first().map { it.name })
    }

    @Test
    fun `a day outside the month is rejected before anything is saved`() = runTest {
        val error = failureOf { create(statementDay = 32) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<CreditCard>(), repository.observeCards().first())
    }

    @Test
    fun `a blank name is rejected before anything is saved`() = runTest {
        val error = failureOf { create(name = " ") }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<CreditCard>(), repository.observeCards().first())
    }

    @Test
    fun `an update changes everything but the currency`() = runTest {
        val id = create()

        repository.updateCard(CreditCard(id, "Bankkart Genc", lira, statementDay = 5, dueDay = 15))
        val error = failureOf {
            repository.updateCard(
                CreditCard(id, "Bankkart Genc", euro, statementDay = 5, dueDay = 15),
            )
        }

        assertEquals(
            listOf(CreditCard(id, "Bankkart Genc", lira, statementDay = 5, dueDay = 15)),
            repository.observeCards().first(),
        )
        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
    }

    @Test
    fun `an archived card is no longer listed but is kept`() = runTest {
        val id = create()

        repository.archiveCard(id)

        assertEquals(emptyList<CreditCard>(), repository.observeCards().first())
        assertEquals(now.toEpochMilli(), database.creditCardDao().findById(id.value)?.archivedAt)
    }
}
