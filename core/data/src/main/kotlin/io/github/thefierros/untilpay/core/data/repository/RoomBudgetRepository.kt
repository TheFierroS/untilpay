package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asInstallmentPlan
import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.data.mapper.effectOn
import io.github.thefierros.untilpay.core.data.mapper.fromRow
import io.github.thefierros.untilpay.core.database.TableChanges
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.BalanceSnapshotDao
import io.github.thefierros.untilpay.core.database.dao.CardStatementDao
import io.github.thefierros.untilpay.core.database.dao.CreditCardDao
import io.github.thefierros.untilpay.core.database.dao.IncomeSourceDao
import io.github.thefierros.untilpay.core.database.dao.TransactionDao
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity
import io.github.thefierros.untilpay.core.domain.AccountBalance
import io.github.thefierros.untilpay.core.domain.BudgetInput
import io.github.thefierros.untilpay.core.domain.CardState
import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.CreditCard
import io.github.thefierros.untilpay.core.model.FixedCost
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import java.time.LocalDate
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class RoomBudgetRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val balanceSnapshotDao: BalanceSnapshotDao,
    private val creditCardDao: CreditCardDao,
    private val cardStatementDao: CardStatementDao,
    private val incomeSourceDao: IncomeSourceDao,
    private val transactionDao: TransactionDao,
    private val tableChanges: TableChanges,
    private val clock: Clock,
) : BudgetRepository {

    override suspend fun loadInput(
        currency: Currency,
        fixedCosts: List<FixedCost>,
        protectedAmount: Money,
    ): BudgetInput {
        val today = LocalDate.now(clock)
        val accounts = accountDao.observeActive().first().map { it.asModel() }
        val snapshots = balanceSnapshotDao.observeLatestPerAccount().first()
            .associateBy { it.accountId }
        val accountsById = accounts.associateBy { it.id }
        val incomeSources = incomeSourceDao.observeActive().first().mapNotNull { row ->
            // An income paid into an archived account no longer counts.
            accountsById[AccountId(row.accountId)]?.let { account -> row.asModel(account.currency) }
        }

        return BudgetInput(
            today = today,
            currency = currency,
            accounts = accounts.map { AccountBalance(it, balanceOf(it, snapshots[it.id.value])) },
            cards = creditCardDao.observeActive().first().map { cardStateOf(it.asModel(), today) },
            incomeSources = incomeSources,
            fixedCosts = fixedCosts,
            protectedAmount = protectedAmount,
        )
    }

    override fun observeInput(
        currency: Currency,
        fixedCosts: List<FixedCost>,
        protectedAmount: Money,
    ): Flow<BudgetInput> = tableChanges.observe(*BUDGET_TABLES)
        .map { loadInput(currency, fixedCosts, protectedAmount) }

    /** The latest snapshot plus everything that happened after it; zero if there is none. */
    private suspend fun balanceOf(account: Account, snapshot: BalanceSnapshotEntity?): Money {
        val accountId = account.id.value
        val after = snapshot?.recordedAt ?: Long.MIN_VALUE
        val change = transactionDao.observeForAccountAfter(accountId, after).first()
            .sumOf { it.effectOn(accountId) }
        return Money((snapshot?.balanceMinor ?: 0) + change, account.currency)
    }

    private suspend fun cardStateOf(card: CreditCard, today: LocalDate): CardState {
        val cardId = card.id.value
        val statements = cardStatementDao.observeForCard(cardId).first()
        val latest = statements.lastOrNull()

        // Spending on the closing day is already on that statement.
        val spendingFrom = latest?.let { startOfDayAfter(it.closingDate) } ?: Long.MIN_VALUE
        val singlePayments = transactionDao.observeCardSpendingSince(cardId, spendingFrom).first()
            .filter { it.installmentCount == null }
            .sumOf { it.amountMinor }
        val plans = transactionDao.observeInstallmentPurchases(cardId).first()
            .map { it.asInstallmentPlan(card.currency, statements, clock.zone) }
            .filter { it.nextInstallment != null }
        val paid = latest?.let { transactionDao.observePaidToward(it.id).first() } ?: 0

        fun money(minorUnits: Long) = Money(minorUnits, card.currency)

        return fromRow(latest?.let { "card statement ${it.id}" } ?: "credit card $cardId") {
            CardState(
                card = card,
                statementBalance = money(latest?.balanceMinor ?: 0),
                minimumPayment = money(latest?.minimumPaymentMinor ?: 0),
                plannedPayment = money(latest?.plannedPaymentMinor ?: 0),
                paidSoFar = money(paid),
                dueDate = latest?.let { LocalDate.ofEpochDay(it.dueDate) }
                    ?: nextDueDate(card, today),
                spentSinceStatement = money(singlePayments),
                installmentPlans = plans,
            )
        }
    }

    private fun startOfDayAfter(epochDay: Long): Long = LocalDate.ofEpochDay(
        epochDay,
    ).plusDays(1).atStartOfDay(clock.zone).toInstant().toEpochMilli()

    /**
     * Before a card's first statement, its next due day from today on. Uses the
     * monthly calendar rule, so day 31 falls on the last day of short months.
     */
    private fun nextDueDate(card: CreditCard, today: LocalDate): LocalDate =
        IncomeSchedule.Monthly(card.dueDay).nextAfter(today.minusDays(1))

    private companion object {

        /** Every table the budget input is read from. */
        val BUDGET_TABLES = arrayOf(
            "accounts",
            "balance_snapshots",
            "credit_cards",
            "card_statements",
            "income_sources",
            "transactions",
        )
    }
}
