package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.data.mapper.toColumns
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.IncomeSourceDao
import io.github.thefierros.untilpay.core.database.model.IncomeSourceEntity
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.IncomeSchedule
import io.github.thefierros.untilpay.core.model.IncomeSource
import io.github.thefierros.untilpay.core.model.IncomeSourceId
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomIncomeRepository @Inject constructor(
    private val incomeSourceDao: IncomeSourceDao,
    private val accountDao: AccountDao,
    private val clock: Clock,
) : IncomeRepository {

    override fun observeIncomeSources(): Flow<List<IncomeSource>> =
        incomeSourceDao.observeActive().map { rows ->
            rows.map { row -> row.asModel(currencyOfAccount(row.accountId)) }
        }

    override suspend fun createIncomeSource(
        name: String,
        expectedAmount: Money,
        schedule: IncomeSchedule,
        accountId: AccountId,
    ): IncomeSourceId {
        // Building the model first applies its rules, such as a positive amount.
        val draft = IncomeSource(IncomeSourceId(0), name, expectedAmount, schedule, accountId)
        requireFitsAccount(draft)
        val columns = draft.schedule.toColumns()
        return IncomeSourceId(
            incomeSourceDao.insert(
                IncomeSourceEntity(
                    name = draft.name,
                    expectedAmountMinor = draft.expectedAmount.minorUnits,
                    accountId = draft.accountId.value,
                    scheduleType = columns.type,
                    scheduleDayOfMonth = columns.dayOfMonth,
                    scheduleDayOfWeek = columns.dayOfWeek,
                    scheduleDate = columns.date,
                    createdAt = clock.millis(),
                ),
            ),
        )
    }

    override suspend fun updateIncomeSource(incomeSource: IncomeSource) {
        val row = existing(incomeSource.id)
        requireFitsAccount(incomeSource)
        val columns = incomeSource.schedule.toColumns()
        incomeSourceDao.update(
            row.copy(
                name = incomeSource.name,
                expectedAmountMinor = incomeSource.expectedAmount.minorUnits,
                accountId = incomeSource.accountId.value,
                scheduleType = columns.type,
                scheduleDayOfMonth = columns.dayOfMonth,
                scheduleDayOfWeek = columns.dayOfWeek,
                scheduleDate = columns.date,
            ),
        )
    }

    override suspend fun archiveIncomeSource(id: IncomeSourceId) {
        incomeSourceDao.archive(existing(id).id, archivedAt = clock.millis())
    }

    /** Checks that the income is paid into an active account, in that account's currency. */
    private suspend fun requireFitsAccount(incomeSource: IncomeSource) {
        val account = requireNotNull(accountDao.findById(incomeSource.accountId.value)) {
            "There is no account ${incomeSource.accountId.value}"
        }
        require(account.archivedAt == null) {
            "Income cannot be paid into archived account ${account.id}"
        }
        require(incomeSource.expectedAmount.currency.currencyCode == account.currencyCode) {
            "Income paid into account ${account.id} must be in ${account.currencyCode}, " +
                "got ${incomeSource.expectedAmount.currency}"
        }
    }

    private suspend fun currencyOfAccount(accountId: Long): Currency {
        val account =
            requireNotNull(accountDao.findById(accountId)) { "There is no account $accountId" }
        return Currency.getInstance(account.currencyCode)
    }

    private suspend fun existing(id: IncomeSourceId): IncomeSourceEntity =
        requireNotNull(incomeSourceDao.findById(id.value)) {
            "There is no income source ${id.value}"
        }
}
