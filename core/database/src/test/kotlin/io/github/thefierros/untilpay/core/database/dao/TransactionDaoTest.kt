package io.github.thefierros.untilpay.core.database.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: TransactionDao

    private var bank = 0L
    private var wallet = 0L
    private var card = 0L
    private var statement = 0L
    private var food = 0L

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.transactionDao()

        runBlocking {
            bank = database.accountDao().insert(account("Ziraat", type = "BANK"))
            wallet = database.accountDao().insert(account("Wallet", type = "CASH"))
            card = database.creditCardDao().insert(
                CreditCardEntity(
                    name = "Bankkart",
                    currencyCode = "TRY",
                    statementDay = 15,
                    dueDay = 25,
                    createdAt = 1_000,
                ),
            )
            statement = database.cardStatementDao().insert(
                CardStatementEntity(
                    cardId = card,
                    closingDate = 20_376,
                    dueDate = 20_386,
                    balanceMinor = 1_000_00,
                    minimumPaymentMinor = 200_00,
                    plannedPaymentMinor = 1_000_00,
                ),
            )
            food = database.categoryDao().insert(CategoryEntity(name = "Food", createdAt = 1_000))
        }
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun account(name: String, type: String) = AccountEntity(
        name = name,
        type = type,
        currencyCode = "TRY",
        includeInBudget = true,
        createdAt = 1_000,
    )

    private fun expense(
        amountMinor: Long,
        at: Long,
        accountId: Long? = null,
        cardId: Long? = null,
        installments: Int? = null,
        categoryId: Long? = food,
    ) = TransactionEntity(
        type = "EXPENSE",
        amountMinor = amountMinor,
        occurredAt = at,
        accountId = accountId,
        cardId = cardId,
        categoryId = categoryId,
        installmentCount = installments,
        createdAt = at,
    )

    private fun transfer(amountMinor: Long, at: Long, from: Long, to: Long) = TransactionEntity(
        type = "TRANSFER",
        amountMinor = amountMinor,
        occurredAt = at,
        accountId = from,
        toAccountId = to,
        createdAt = at,
    )

    private fun payment(amountMinor: Long, at: Long) = TransactionEntity(
        type = "CARD_PAYMENT",
        amountMinor = amountMinor,
        occurredAt = at,
        accountId = bank,
        cardStatementId = statement,
        createdAt = at,
    )

    @Test
    fun `an inserted transaction can be read back with its generated id`() = runTest {
        val lunch = expense(45_00, at = 5_000, accountId = bank).copy(note = "Lunch")

        val id = dao.insert(lunch)

        assertEquals(lunch.copy(id = id), dao.findById(id))
    }

    @Test
    fun `a deleted transaction is gone`() = runTest {
        val id = dao.insert(expense(45_00, at = 5_000, accountId = bank))

        dao.delete(checkNotNull(dao.findById(id)))

        assertNull(dao.findById(id))
    }

    @Test
    fun `transactions in a time range are listed newest first`() = runTest {
        dao.insert(expense(10_00, at = 1_000, accountId = bank))
        dao.insert(expense(20_00, at = 2_000, accountId = bank))
        dao.insert(expense(25_00, at = 2_500, accountId = bank))
        dao.insert(expense(30_00, at = 3_000, accountId = bank))

        val amounts = dao.observeBetween(start = 2_000, end = 3_000).first().map { it.amountMinor }

        assertEquals(listOf(25_00L, 20_00L), amounts)
    }

    @Test
    fun `an account sees money going out and coming in after a moment`() = runTest {
        dao.insert(expense(10_00, at = 1_000, accountId = bank))
        dao.insert(expense(15_00, at = 2_000, accountId = bank))
        dao.insert(expense(20_00, at = 3_000, accountId = bank))
        dao.insert(transfer(30_00, at = 4_000, from = bank, to = wallet))
        dao.insert(expense(40_00, at = 5_000, accountId = wallet))
        dao.insert(payment(50_00, at = 6_000))

        val bankAmounts = dao.observeForAccountAfter(bank, after = 2_000).first()
            .map { it.amountMinor }
        val walletAmounts = dao.observeForAccountAfter(wallet, after = 2_000).first()
            .map { it.amountMinor }

        assertEquals(listOf(20_00L, 30_00L, 50_00L), bankAmounts)
        assertEquals(listOf(30_00L, 40_00L), walletAmounts)
    }

    @Test
    fun `card spending since a moment includes installment purchases but not other spending`() =
        runTest {
            dao.insert(expense(10_00, at = 1_000, cardId = card))
            dao.insert(expense(20_00, at = 3_000, cardId = card))
            dao.insert(expense(300_00, at = 4_000, cardId = card, installments = 3))
            dao.insert(expense(40_00, at = 5_000, accountId = bank))

            val amounts = dao.observeCardSpendingSince(card, since = 3_000).first()
                .map { it.amountMinor }

            assertEquals(listOf(20_00L, 300_00L), amounts)
        }

    @Test
    fun `installment purchases are listed whenever they were made`() = runTest {
        dao.insert(expense(600_00, at = 1_000, cardId = card, installments = 6))
        dao.insert(expense(20_00, at = 2_000, cardId = card))
        dao.insert(expense(300_00, at = 3_000, cardId = card, installments = 3))

        val purchases = dao.observeInstallmentPurchases(card).first()

        assertEquals(listOf(600_00L, 300_00L), purchases.map { it.amountMinor })
        assertEquals(listOf(6, 3), purchases.map { it.installmentCount })
    }

    @Test
    fun `payments toward a statement add up and start at zero`() = runTest {
        assertEquals(0L, dao.observePaidToward(statement).first())

        dao.insert(payment(300_00, at = 1_000))
        dao.insert(payment(200_00, at = 2_000))
        dao.insert(expense(40_00, at = 3_000, accountId = bank))

        assertEquals(500_00L, dao.observePaidToward(statement).first())
    }

    @Test
    fun `a transaction in a missing category is rejected`() = runTest {
        val error = runCatching {
            dao.insert(expense(45_00, at = 5_000, accountId = bank, categoryId = 42))
        }.exceptionOrNull()

        assertTrue(
            "Expected a foreign key violation but got $error",
            error is SQLiteConstraintException,
        )
    }
}
