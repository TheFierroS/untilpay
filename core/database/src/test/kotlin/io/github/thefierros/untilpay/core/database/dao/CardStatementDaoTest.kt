package io.github.thefierros.untilpay.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardStatementDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: CardStatementDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.cardStatementDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun insertCard(name: String): Long = database.creditCardDao().insert(
        CreditCardEntity(
            name = name,
            currencyCode = "TRY",
            statementDay = 15,
            dueDay = 25,
            createdAt = 1_000,
        ),
    )

    private fun day(month: Int, dayOfMonth: Int): Long =
        LocalDate.of(2026, month, dayOfMonth).toEpochDay()

    private fun statement(cardId: Long, closedOn: Long, balanceMinor: Long) = CardStatementEntity(
        cardId = cardId,
        closingDate = closedOn,
        dueDate = closedOn + 10,
        balanceMinor = balanceMinor,
        minimumPaymentMinor = balanceMinor / 5,
        plannedPaymentMinor = balanceMinor,
    )

    @Test
    fun `the latest statement is the one with the latest closing date`() = runTest {
        val card = insertCard("Bankkart")
        dao.insert(statement(card, closedOn = day(8, 15), balanceMinor = 1_000_00))
        dao.insert(statement(card, closedOn = day(10, 15), balanceMinor = 3_000_00))
        dao.insert(statement(card, closedOn = day(9, 15), balanceMinor = 2_000_00))

        assertEquals(3_000_00L, dao.observeLatest(card).first()?.balanceMinor)
    }

    @Test
    fun `a card without statements has no latest statement`() = runTest {
        val card = insertCard("Bankkart")

        assertNull(dao.observeLatest(card).first())
    }

    @Test
    fun `each card gets only its own latest statement`() = runTest {
        val first = insertCard("Bankkart")
        val second = insertCard("Other")
        dao.insert(statement(first, closedOn = day(9, 15), balanceMinor = 1_000_00))
        dao.insert(statement(second, closedOn = day(10, 1), balanceMinor = 500_00))
        dao.insert(statement(first, closedOn = day(10, 15), balanceMinor = 1_200_00))

        val latest = dao.observeLatestPerCard().first()

        assertEquals(listOf(first, second), latest.map { it.cardId })
        assertEquals(listOf(1_200_00L, 500_00L), latest.map { it.balanceMinor })
    }

    @Test
    fun `a changed planned payment is saved`() = runTest {
        val card = insertCard("Bankkart")
        val id = dao.insert(statement(card, closedOn = day(10, 15), balanceMinor = 1_000_00))
        val original = checkNotNull(dao.findById(id))

        dao.update(original.copy(plannedPaymentMinor = 400_00))

        assertEquals(400_00L, dao.findById(id)?.plannedPaymentMinor)
    }

    @Test
    fun `a second statement with the same closing date is rejected`() = runTest {
        val card = insertCard("Bankkart")
        dao.insert(statement(card, closedOn = day(10, 15), balanceMinor = 1_000_00))

        val error = runCatching {
            dao.insert(statement(card, closedOn = day(10, 15), balanceMinor = 2_000_00))
        }.exceptionOrNull()

        assertTrue(
            "Expected a unique constraint violation but got $error",
            error is SQLiteConstraintException,
        )
    }

    @Test
    fun `a statement for a missing card is rejected`() = runTest {
        val error = runCatching {
            dao.insert(statement(cardId = 42, closedOn = day(10, 15), balanceMinor = 1_000_00))
        }.exceptionOrNull()

        assertTrue(
            "Expected a foreign key violation but got $error",
            error is SQLiteConstraintException,
        )
    }

    @Test
    fun `a card's statements are listed oldest first without other cards' statements`() = runTest {
        val card = insertCard("Bankkart")
        val other = insertCard("Other")
        dao.insert(statement(card, closedOn = day(10, 15), balanceMinor = 2_000_00))
        dao.insert(statement(other, closedOn = day(9, 1), balanceMinor = 500_00))
        dao.insert(statement(card, closedOn = day(9, 15), balanceMinor = 1_000_00))

        val closings = dao.observeForCard(card).first().map { it.closingDate }

        assertEquals(listOf(day(9, 15), day(10, 15)), closings)
    }
}
