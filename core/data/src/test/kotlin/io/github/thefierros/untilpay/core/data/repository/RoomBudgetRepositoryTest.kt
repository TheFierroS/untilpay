package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.TableChanges
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity
import io.github.thefierros.untilpay.core.database.model.CardStatementEntity
import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.database.model.IncomeSourceEntity
import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import io.github.thefierros.untilpay.core.domain.BudgetInput
import io.github.thefierros.untilpay.core.domain.InstallmentPlan
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomBudgetRepositoryTest {

    private val zone = ZoneId.of("Europe/Istanbul")
    private val lira = Currency.getInstance("TRY")
    private val today = LocalDate.of(2026, 10, 18)

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomBudgetRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomBudgetRepository(
            accountDao = database.accountDao(),
            balanceSnapshotDao = database.balanceSnapshotDao(),
            creditCardDao = database.creditCardDao(),
            cardStatementDao = database.cardStatementDao(),
            incomeSourceDao = database.incomeSourceDao(),
            transactionDao = database.transactionDao(),
            tableChanges = TableChanges(database),
            clock = Clock.fixed(today.atTime(12, 0).atZone(zone).toInstant(), zone),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    /** Epoch milliseconds of [hour]:00 on [day] in Istanbul. */
    private fun at(day: LocalDate, hour: Int = 12): Long =
        day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()

    private fun october(dayOfMonth: Int) = LocalDate.of(2026, 10, dayOfMonth)

    private suspend fun load() =
        repository.loadInput(lira, fixedCosts = tl(0), protectedAmount = tl(0))

    private suspend fun account(name: String, type: String = "BANK"): Long =
        database.accountDao().insert(
            AccountEntity(
                name = name,
                type = type,
                currencyCode = "TRY",
                includeInBudget = true,
                createdAt = 1_000,
            ),
        )

    private suspend fun snapshot(accountId: Long, balanceMinor: Long, at: Long) {
        database.balanceSnapshotDao().insert(
            BalanceSnapshotEntity(
                accountId = accountId,
                balanceMinor = balanceMinor,
                recordedAt = at,
            ),
        )
    }

    private suspend fun card(): Long = database.creditCardDao().insert(
        CreditCardEntity(
            name = "Bankkart",
            currencyCode = "TRY",
            statementDay = 15,
            dueDay = 25,
            createdAt = 1_000,
        ),
    )

    private suspend fun statement(
        cardId: Long,
        closing: LocalDate,
        balanceMinor: Long = 1_000_00,
        minimumMinor: Long = 200_00,
        plannedMinor: Long = 1_000_00,
    ): Long = database.cardStatementDao().insert(
        CardStatementEntity(
            cardId = cardId,
            closingDate = closing.toEpochDay(),
            dueDate = closing.plusDays(10).toEpochDay(),
            balanceMinor = balanceMinor,
            minimumPaymentMinor = minimumMinor,
            plannedPaymentMinor = plannedMinor,
        ),
    )

    private suspend fun record(
        type: String,
        amountMinor: Long,
        at: Long,
        accountId: Long? = null,
        toAccountId: Long? = null,
        cardId: Long? = null,
        statementId: Long? = null,
        installments: Int? = null,
    ) {
        database.transactionDao().insert(
            TransactionEntity(
                type = type,
                amountMinor = amountMinor,
                occurredAt = at,
                accountId = accountId,
                toAccountId = toAccountId,
                cardId = cardId,
                cardStatementId = statementId,
                installmentCount = installments,
                createdAt = at,
            ),
        )
    }

    @Test
    fun `the budget day comes from the clock`() = runTest {
        assertEquals(today, load().today)
    }

    @Test
    fun `a balance is the latest snapshot plus what happened after it`() = runTest {
        val bank = account("Ziraat")
        record("EXPENSE", 99_00, at(october(10), hour = 9), accountId = bank)
        snapshot(bank, 4_000_00, at(october(10), hour = 10))
        record("EXPENSE", 150_00, at(october(10), hour = 11), accountId = bank)
        record("INCOME", 1_000_00, at(october(10), hour = 12), accountId = bank)

        assertEquals(tl(4_850_00), load().accounts.single().balance)
    }

    @Test
    fun `an account without snapshots starts from zero`() = runTest {
        val bank = account("Ziraat")
        record("INCOME", 300_00, at(october(10)), accountId = bank)

        assertEquals(tl(300_00), load().accounts.single().balance)
    }

    @Test
    fun `transfers and card payments move money between accounts`() = runTest {
        val bank = account("Ziraat")
        val wallet = account("Wallet", type = "CASH")
        val statementId = statement(card(), closing = october(15))
        snapshot(bank, 1_000_00, at(october(16), hour = 10))
        snapshot(wallet, 50_00, at(october(16), hour = 10))
        record(
            "TRANSFER",
            200_00,
            at(october(16), hour = 11),
            accountId = bank,
            toAccountId = wallet,
        )
        record(
            "CARD_PAYMENT",
            300_00,
            at(october(16), hour = 12),
            accountId = bank,
            statementId = statementId,
        )

        assertEquals(listOf(tl(500_00), tl(250_00)), load().accounts.map { it.balance })
    }

    @Test
    fun `a card without statements counts all its spending toward the next one`() = runTest {
        val cardId = card()
        record("EXPENSE", 120_00, at(october(1)), cardId = cardId)
        record("EXPENSE", 80_00, at(october(17)), cardId = cardId)

        val state = load().cards.single()

        assertEquals(tl(0), state.statementBalance)
        assertEquals(tl(0), state.paidSoFar)
        assertEquals(tl(200_00), state.spentSinceStatement)
        assertEquals(october(25), state.dueDate)
    }

    @Test
    fun `a card's state comes from its latest statement and what happened after it`() = runTest {
        val bank = account("Ziraat")
        val cardId = card()
        statement(cardId, closing = LocalDate.of(2026, 9, 15), balanceMinor = 9_999_00)
        val latest = statement(
            cardId,
            closing = october(15),
            balanceMinor = 2_000_00,
            minimumMinor = 400_00,
            plannedMinor = 1_500_00,
        )
        record("CARD_PAYMENT", 500_00, at(october(16)), accountId = bank, statementId = latest)
        record("CARD_PAYMENT", 300_00, at(october(17)), accountId = bank, statementId = latest)
        record("EXPENSE", 70_00, at(october(15), hour = 20), cardId = cardId)
        record("EXPENSE", 40_00, at(october(16), hour = 9), cardId = cardId)
        record("EXPENSE", 25_00, at(october(17)), cardId = cardId)

        val state = load().cards.single()

        assertEquals(tl(2_000_00), state.statementBalance)
        assertEquals(tl(400_00), state.minimumPayment)
        assertEquals(tl(1_500_00), state.plannedPayment)
        assertEquals(tl(800_00), state.paidSoFar)
        assertEquals(october(25), state.dueDate)
        assertEquals(tl(65_00), state.spentSinceStatement)
    }

    @Test
    fun `installments are counted as billed by the statements after the purchase`() = runTest {
        val cardId = card()
        statement(cardId, closing = LocalDate.of(2026, 8, 15))
        statement(cardId, closing = LocalDate.of(2026, 9, 15))
        statement(cardId, closing = october(15))
        record("EXPENSE", 300_00, at(LocalDate.of(2026, 7, 20)), cardId = cardId, installments = 3)
        record("EXPENSE", 600_00, at(LocalDate.of(2026, 9, 1)), cardId = cardId, installments = 6)
        record("EXPENSE", 90_00, at(october(16)), cardId = cardId, installments = 3)
        record("EXPENSE", 10_00, at(october(17)), cardId = cardId)

        val state = load().cards.single()

        assertEquals(
            listOf(InstallmentPlan(tl(600_00), 6, 2), InstallmentPlan(tl(90_00), 3, 0)),
            state.installmentPlans,
        )
        assertEquals(tl(10_00), state.spentSinceStatement)
    }

    @Test
    fun `archived accounts and the income paid into them do not count`() = runTest {
        val bank = account("Ziraat")
        val closed = account("Old bank")
        database.accountDao().archive(closed, archivedAt = 2_000)
        for (accountId in listOf(bank, closed)) {
            database.incomeSourceDao().insert(
                IncomeSourceEntity(
                    name = "Allowance",
                    expectedAmountMinor = 5_000_00,
                    accountId = accountId,
                    scheduleType = "MONTHLY",
                    scheduleDayOfMonth = 15,
                    createdAt = 1_000,
                ),
            )
        }

        val input = load()

        assertEquals(listOf("Ziraat"), input.accounts.map { it.account.name })
        val income = input.incomeSources.single()
        assertEquals(tl(5_000_00), income.expectedAmount)
        assertEquals(IncomeSchedule.Monthly(15), income.schedule)
    }

    @Test
    fun `the budget input is read again when the money changes`() = runTest {
        val inputs = Channel<BudgetInput>(Channel.UNLIMITED)
        val collector = launch {
            repository.observeInput(lira, fixedCosts = tl(0), protectedAmount = tl(0))
                .collect { inputs.send(it) }
        }
        assertEquals(emptyList<Any>(), inputs.receive().accounts)

        val bank = account("Ziraat")
        snapshot(bank, 100_00, at(october(17)))

        val updated = inputs.receiveUntil { input ->
            input.accounts.singleOrNull()?.balance == tl(100_00)
        }
        assertEquals(listOf("Ziraat"), updated.accounts.map { it.account.name })
        collector.cancel()
    }

    /** Waits for the first input that satisfies [condition]. */
    private suspend fun Channel<BudgetInput>.receiveUntil(
        condition: (BudgetInput) -> Boolean,
    ): BudgetInput {
        while (true) {
            val input = receive()
            if (condition(input)) return input
        }
    }
}
