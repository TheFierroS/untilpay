package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.AccountType
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
class RoomAccountRepositoryTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")
    private val now = Instant.parse("2026-10-18T09:00:00Z")

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomAccountRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomAccountRepository(
            accountDao = database.accountDao(),
            balanceSnapshotDao = database.balanceSnapshotDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    private suspend fun create(
        name: String = "Ziraat",
        currency: Currency = lira,
        openingBalance: Money = tl(1_000_00),
    ): AccountId = repository.createAccount(
        name = name,
        type = AccountType.BANK,
        currency = currency,
        includeInBudget = true,
        openingBalance = openingBalance,
    )

    private suspend fun latestBalanceOf(id: AccountId): Long? =
        database.balanceSnapshotDao().observeLatest(id.value).first()?.balanceMinor

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        runCatching { block() }.exceptionOrNull()

    @Test
    fun `a created account is listed and its opening balance is recorded`() = runTest {
        val id = create()

        assertEquals(
            listOf(Account(id, "Ziraat", AccountType.BANK, lira, includeInBudget = true)),
            repository.observeAccounts().first(),
        )
        val snapshot = database.balanceSnapshotDao().observeLatest(id.value).first()
        assertEquals(1_000_00L, snapshot?.balanceMinor)
        assertEquals(now.toEpochMilli(), snapshot?.recordedAt)
    }

    @Test
    fun `accounts are listed in creation order`() = runTest {
        create(name = "Ziraat")
        create(name = "Wallet")

        assertEquals(
            listOf("Ziraat", "Wallet"),
            repository.observeAccounts().first().map {
                it.name
            },
        )
    }

    @Test
    fun `a blank name is rejected before anything is saved`() = runTest {
        val error = failureOf { create(name = " ") }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<Account>(), repository.observeAccounts().first())
    }

    @Test
    fun `an opening balance in another currency is rejected`() = runTest {
        val error = failureOf { create(openingBalance = Money(10_00, euro)) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<Account>(), repository.observeAccounts().first())
    }

    @Test
    fun `a recorded balance becomes the latest one`() = runTest {
        val id = create()

        repository.recordBalance(id, tl(750_00))

        assertEquals(750_00L, latestBalanceOf(id))
    }

    @Test
    fun `a balance in another currency is rejected`() = runTest {
        val id = create()

        val error = failureOf { repository.recordBalance(id, Money(10_00, euro)) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(1_000_00L, latestBalanceOf(id))
    }

    @Test
    fun `an update changes everything but the currency`() = runTest {
        val id = create()

        repository.updateAccount(Account(id, "Ziraat Main", AccountType.CASH, lira, false))
        val error = failureOf {
            repository.updateAccount(Account(id, "Ziraat Main", AccountType.CASH, euro, false))
        }

        assertEquals(
            listOf(Account(id, "Ziraat Main", AccountType.CASH, lira, includeInBudget = false)),
            repository.observeAccounts().first(),
        )
        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
    }

    @Test
    fun `an archived account is no longer listed but is kept`() = runTest {
        val id = create()

        repository.archiveAccount(id)

        assertEquals(emptyList<Account>(), repository.observeAccounts().first())
        assertEquals(now.toEpochMilli(), database.accountDao().findById(id.value)?.archivedAt)
    }
}
