package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CardStatement
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.Money
import io.github.thefierros.untilpay.core.model.StatementId
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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
    private val closing = LocalDate.of(2026, 10, 15)

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
            cardStatementDao = database.cardStatementDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    private suspend fun create(
        name: String = "Bankkart",
        statementDay: Int = 15,
        dueDay: Int = 25,
    ): CardId = repository.createCard(name, lira, statementDay, dueDay)

    private suspend fun record(
        cardId: CardId,
        closingDate: LocalDate = closing,
        currency: Currency = lira,
    ): StatementId = repository.recordStatement(
        cardId = cardId,
        closingDate = closingDate,
        dueDate = closingDate.plusDays(10),
        balance = Money(2_000_00, currency),
        minimumPayment = Money(400_00, currency),
        plannedPayment = Money(2_000_00, currency),
    )

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

    @Test
    fun `recorded statements are listed oldest first`() = runTest {
        val card = create()
        val october = record(card, closingDate = closing)
        val september = record(card, closingDate = closing.minusMonths(1))

        val statements = repository.observeStatements(card).first()

        assertEquals(listOf(september, october), statements.map { it.id })
        assertEquals(
            CardStatement(
                id = october,
                cardId = card,
                closingDate = closing,
                dueDate = closing.plusDays(10),
                balance = tl(2_000_00),
                minimumPayment = tl(400_00),
                plannedPayment = tl(2_000_00),
            ),
            statements.last(),
        )
    }

    @Test
    fun `a statement in another currency than its card is rejected`() = runTest {
        val card = create()

        val error = failureOf { record(card, currency = euro) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<CardStatement>(), repository.observeStatements(card).first())
    }

    @Test
    fun `a second statement with the same closing date is rejected clearly`() = runTest {
        val card = create()
        record(card)

        val error = failureOf { record(card) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(1, repository.observeStatements(card).first().size)
    }

    @Test
    fun `a statement can be corrected but not moved onto another statement's date`() = runTest {
        val card = create()
        record(card, closingDate = closing.minusMonths(1))
        record(card, closingDate = closing)
        val latest = repository.observeStatements(card).first().last()

        repository.updateStatement(latest.copy(plannedPayment = tl(500_00)))
        val clash = failureOf {
            repository.updateStatement(latest.copy(closingDate = closing.minusMonths(1)))
        }

        assertEquals(tl(500_00), repository.observeStatements(card).first().last().plannedPayment)
        assertTrue("Expected a rejection but got $clash", clash is IllegalArgumentException)
    }
}
