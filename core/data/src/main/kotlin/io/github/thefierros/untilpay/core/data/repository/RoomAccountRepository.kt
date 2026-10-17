package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.database.dao.AccountDao
import io.github.thefierros.untilpay.core.database.dao.BalanceSnapshotDao
import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.database.model.BalanceSnapshotEntity
import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.AccountType
import io.github.thefierros.untilpay.core.model.Money
import java.time.Clock
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomAccountRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val balanceSnapshotDao: BalanceSnapshotDao,
    private val clock: Clock,
) : AccountRepository {

    override fun observeAccounts(): Flow<List<Account>> =
        accountDao.observeActive().map { rows -> rows.map { it.asModel() } }

    override suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        includeInBudget: Boolean,
        openingBalance: Money,
    ): AccountId {
        // Building the model first applies its rules, such as a non-blank name.
        val draft = Account(AccountId(0), name, type, currency, includeInBudget)
        require(openingBalance.currency == currency) {
            "The opening balance must be in $currency, got ${openingBalance.currency}"
        }
        val id = AccountId(
            accountDao.insert(
                AccountEntity(
                    name = draft.name,
                    type = draft.type.name,
                    currencyCode = draft.currency.currencyCode,
                    includeInBudget = draft.includeInBudget,
                    createdAt = clock.millis(),
                ),
            ),
        )
        // If the app stops before this line, the account simply starts at zero.
        recordBalance(id, openingBalance)
        return id
    }

    override suspend fun updateAccount(account: Account) {
        val row = existing(account.id)
        require(account.currency.currencyCode == row.currencyCode) {
            "The currency of account ${row.id} cannot change"
        }
        accountDao.update(
            row.copy(
                name = account.name,
                type = account.type.name,
                includeInBudget = account.includeInBudget,
            ),
        )
    }

    override suspend fun recordBalance(id: AccountId, balance: Money) {
        val row = existing(id)
        require(balance.currency.currencyCode == row.currencyCode) {
            "A balance of account ${row.id} must be in ${row.currencyCode}, got ${balance.currency}"
        }
        balanceSnapshotDao.insert(
            BalanceSnapshotEntity(
                accountId = id.value,
                balanceMinor = balance.minorUnits,
                recordedAt = clock.millis(),
            ),
        )
    }

    override suspend fun archiveAccount(id: AccountId) {
        accountDao.archive(existing(id).id, archivedAt = clock.millis())
    }

    private suspend fun existing(id: AccountId): AccountEntity =
        requireNotNull(accountDao.findById(id.value)) { "There is no account ${id.value}" }
}
