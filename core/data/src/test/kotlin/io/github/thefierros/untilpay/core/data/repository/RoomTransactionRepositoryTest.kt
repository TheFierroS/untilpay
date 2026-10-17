package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CategoryId
import io.github.thefierros.untilpay.core.model.Money
import io.github.thefierros.untilpay.core.model.PaymentSource
import io.github.thefierros.untilpay.core.model.StatementId
import io.github.thefierros.untilpay.core.model.Transaction
import io.github.thefierros.untilpay.core.model.TransactionId
import java.time.Clock
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
class RoomTransactionRepositoryTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")
    private val now = Instant.parse("2026-10-18T09:00:00Z")

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomTransactionRepository

    private var bank = AccountId(0)
    private var wallet = AccountId(0)
    private var euroAccount = AccountId(0)
    private var card = CardId(0)
    private var statement = StatementId(0)
    private var food = CategoryId(0)

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomTransactionRepository(
            transactionDao = database.transactionDao(),
            accountDao = database.accountDao(),
            creditCardDao = database.creditCardDao(),
            cardStatementDao = database.cardStatementDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
        runBlocking {
            bank = AccountId(insertAccount("Ziraat", currencyCode = "TRY"))
            wallet = AccountId(insertAccount("Wallet", currencyCode = "TRY"))
            euroAccount = AccountId(insertAccount("Euro savings", currencyCode = "EUR"))
            card = CardId(
                database.creditCardDao().insert(
                    CreditCardEntity(
                        name = "Bankkart",
                        currencyCode = "TRY",
                        statementDay = 15,
                        dueDay = 25,
                        createdAt = 1_000,
                    ),
                ),
            )
            statement = StatementId(
                database.cardStatementDao().insert(
                    CardStatementEntity(
                        cardId = card.value,
                        closingDate = 20_376,
                        dueDate = 20_386,
                        balanceMinor = 1_000_00,
                        minimumPaymentMinor = 200_00,
                        plannedPaymentMinor = 1_000_00,
                    ),
                ),
            )
            food = CategoryId(
                database.categoryDao().insert(CategoryEntity(name = "Food", createdAt = 1_000)),
            )
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

    /** [n] minutes after now. */
    private fun minute(n: Long): Instant = now.plusSeconds(n * 60)

    private fun lunch(at: Instant = minute(1), amount: Money = tl(45_00)) = Transaction.Expense(
        id = TransactionId(0),
        amount = amount,
        occurredAt = at,
        source = PaymentSource.FromAccount(bank),
        categoryId = food,
        note = "Lunch",
    )

    private suspend fun listed(): List<Transaction> =
        repository.observeBetween(minute(0), minute(10)).first()

    private fun Transaction.withId(id: TransactionId): Transaction = when (this) {
        is Transaction.Expense -> copy(id = id)
        is Transaction.Income -> copy(id = id)
        is Transaction.Transfer -> copy(id = id)
        is Transaction.CardPayment -> copy(id = id)
    }

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        runCatching { block() }.exceptionOrNull()

    @Test
    fun `every kind of transaction is recorded and read back newest first`() = runTest {
        val drafts = listOf(
            lunch(at = minute(1)),
            Transaction.Expense(
                id = TransactionId(0),
                amount = tl(900_00),
                occurredAt = minute(2),
                source = PaymentSource.OnCard(card, installments = 3),
            ),
            Transaction.Income(TransactionId(0), tl(5_000_00), minute(3), accountId = bank),
            Transaction.Transfer(
                TransactionId(0),
                tl(200_00),
                minute(4),
                bank,
                toAccountId = wallet,
            ),
            Transaction.CardPayment(TransactionId(0), tl(300_00), minute(5), bank, statement),
        )

        val recorded = drafts.map { it.withId(repository.record(it)) }

        assertEquals(recorded.reversed(), listed())
    }

    @Test
    fun `transactions outside the range are left out`() = runTest {
        repository.record(lunch(at = minute(1)))
        val later = repository.record(lunch(at = minute(3)))

        val inRange = repository.observeBetween(minute(2), minute(4)).first()

        assertEquals(listOf(later), inRange.map { it.id })
    }

    @Test
    fun `an amount in another currency than its account is rejected`() = runTest {
        val error = failureOf { repository.record(lunch(amount = Money(45_00, euro))) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<Transaction>(), listed())
    }

    @Test
    fun `a transfer between accounts in different currencies is rejected`() = runTest {
        val transfer = Transaction.Transfer(
            id = TransactionId(0),
            amount = tl(100_00),
            occurredAt = minute(1),
            fromAccountId = bank,
            toAccountId = euroAccount,
        )

        val error = failureOf { repository.record(transfer) }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<Transaction>(), listed())
    }

    @Test
    fun `a corrected transaction is saved`() = runTest {
        val id = repository.record(lunch())

        val corrected = lunch(amount = tl(54_00)).copy(id = id)
        repository.update(corrected)

        assertEquals(listOf(corrected), listed())
    }

    @Test
    fun `a deleted transaction is gone`() = runTest {
        val id = repository.record(lunch())

        repository.delete(id)

        assertEquals(emptyList<Transaction>(), listed())
    }
}
