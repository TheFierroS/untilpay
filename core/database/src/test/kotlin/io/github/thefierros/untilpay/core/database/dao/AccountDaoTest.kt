package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: AccountDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.accountDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun account(name: String, createdAt: Long) = AccountEntity(
        name = name,
        type = "BANK",
        currencyCode = "TRY",
        includeInBudget = true,
        createdAt = createdAt,
    )

    @Test
    fun `an inserted account can be read back with its generated id`() = runTest {
        val id = dao.insert(account("Ziraat", createdAt = 1_000))

        assertEquals(account("Ziraat", createdAt = 1_000).copy(id = id), dao.findById(id))
    }

    @Test
    fun `a missing account is null`() = runTest {
        assertNull(dao.findById(42))
    }

    @Test
    fun `active accounts are listed in creation order`() = runTest {
        dao.insert(account("Wallet", createdAt = 2_000))
        dao.insert(account("Ziraat", createdAt = 1_000))

        val names = dao.observeActive().first().map { it.name }

        assertEquals(listOf("Ziraat", "Wallet"), names)
    }

    @Test
    fun `an archived account is kept but no longer listed as active`() = runTest {
        val id = dao.insert(account("Old bank", createdAt = 1_000))

        dao.archive(id, archivedAt = 5_000)

        assertTrue(dao.observeActive().first().isEmpty())
        assertEquals(5_000L, dao.findById(id)?.archivedAt)
    }

    @Test
    fun `changes to an account are saved`() = runTest {
        val id = dao.insert(account("Ziraat", createdAt = 1_000))
        val original = checkNotNull(dao.findById(id))

        dao.update(original.copy(name = "Ziraat Main", includeInBudget = false))

        val updated = checkNotNull(dao.findById(id))
        assertEquals("Ziraat Main", updated.name)
        assertFalse(updated.includeInBudget)
    }
}
