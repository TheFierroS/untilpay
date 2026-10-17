package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreditCardDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: CreditCardDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.creditCardDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun card(name: String, createdAt: Long) = CreditCardEntity(
        name = name,
        currencyCode = "TRY",
        statementDay = 15,
        dueDay = 25,
        createdAt = createdAt,
    )

    @Test
    fun `an inserted card can be read back with its generated id`() = runTest {
        val id = dao.insert(card("Bankkart", createdAt = 1_000))

        assertEquals(card("Bankkart", createdAt = 1_000).copy(id = id), dao.findById(id))
    }

    @Test
    fun `a missing card is null`() = runTest {
        assertNull(dao.findById(42))
    }

    @Test
    fun `active cards are listed in creation order without archived ones`() = runTest {
        dao.insert(card("Second", createdAt = 2_000))
        val closed = dao.insert(card("Closed", createdAt = 1_500))
        dao.insert(card("First", createdAt = 1_000))

        dao.archive(closed, archivedAt = 3_000)

        val names = dao.observeActive().first().map { it.name }
        assertEquals(listOf("First", "Second"), names)
        assertEquals(3_000L, dao.findById(closed)?.archivedAt)
    }

    @Test
    fun `changes to a card are saved`() = runTest {
        val id = dao.insert(card("Bankkart", createdAt = 1_000))
        val original = checkNotNull(dao.findById(id))

        dao.update(original.copy(statementDay = 5, dueDay = 15))

        val updated = checkNotNull(dao.findById(id))
        assertEquals(5, updated.statementDay)
        assertEquals(15, updated.dueDay)
    }
}
