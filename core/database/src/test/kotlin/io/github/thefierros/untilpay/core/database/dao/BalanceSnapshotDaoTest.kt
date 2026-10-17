package io.github.thefierros.untilpay.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity
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
class BalanceSnapshotDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: BalanceSnapshotDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.balanceSnapshotDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun insertAccount(name: String): Long = database.accountDao().insert(
        AccountEntity(
            name = name,
            type = "BANK",
            currencyCode = "TRY",
            includeInBudget = true,
            createdAt = 1_000,
        ),
    )

    private suspend fun record(accountId: Long, balanceMinor: Long, recordedAt: Long) {
        dao.insert(
            BalanceSnapshotEntity(
                accountId = accountId,
                balanceMinor = balanceMinor,
                recordedAt = recordedAt,
            ),
        )
    }

    @Test
    fun `the latest snapshot is the most recently recorded one`() = runTest {
        val account = insertAccount("Ziraat")
        record(account, balanceMinor = 100_00, recordedAt = 1_000)
        record(account, balanceMinor = 300_00, recordedAt = 3_000)
        record(account, balanceMinor = 200_00, recordedAt = 2_000)

        assertEquals(300_00L, dao.observeLatest(account).first()?.balanceMinor)
    }

    @Test
    fun `of two snapshots recorded at the same time the later insert wins`() = runTest {
        val account = insertAccount("Ziraat")
        record(account, balanceMinor = 100_00, recordedAt = 1_000)
        record(account, balanceMinor = 150_00, recordedAt = 1_000)

        assertEquals(150_00L, dao.observeLatest(account).first()?.balanceMinor)
    }

    @Test
    fun `an account without snapshots has no latest snapshot`() = runTest {
        val account = insertAccount("Ziraat")

        assertNull(dao.observeLatest(account).first())
    }

    @Test
    fun `each account gets only its own latest snapshot`() = runTest {
        val bank = insertAccount("Ziraat")
        val wallet = insertAccount("Wallet")
        val empty = insertAccount("Unused")
        record(bank, balanceMinor = 4_000_00, recordedAt = 1_000)
        record(wallet, balanceMinor = 50_00, recordedAt = 1_500)
        record(bank, balanceMinor = 4_250_00, recordedAt = 2_000)

        val latest = dao.observeLatestPerAccount().first()

        assertEquals(listOf(bank, wallet), latest.map { it.accountId })
        assertEquals(listOf(4_250_00L, 50_00L), latest.map { it.balanceMinor })
        assertTrue(latest.none { it.accountId == empty })
    }

    @Test
    fun `a snapshot for a missing account is rejected`() = runTest {
        val error = runCatching {
            record(accountId = 42, balanceMinor = 100_00, recordedAt = 1_000)
        }.exceptionOrNull()

        assertTrue(
            "Expected a foreign key violation but got $error",
            error is SQLiteConstraintException,
        )
    }
}
