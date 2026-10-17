package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.AccountType
import io.github.thefierros.untilpay.core.model.Money
import java.util.Currency
import kotlinx.coroutines.flow.Flow

/** Creates and changes the places where the user's money sits. */
interface AccountRepository {

    /** Active accounts in the order they were created. */
    fun observeAccounts(): Flow<List<Account>>

    /**
     * Creates an account whose first balance is [openingBalance] and returns its
     * id. The opening balance must be in [currency].
     */
    suspend fun createAccount(
        name: String,
        type: AccountType,
        currency: Currency,
        includeInBudget: Boolean,
        openingBalance: Money,
    ): AccountId

    /** Saves a changed name, type or budget setting. An account's currency never changes. */
    suspend fun updateAccount(account: Account)

    /** Records the balance the account has right now, in its own currency. */
    suspend fun recordBalance(id: AccountId, balance: Money)

    /** Hides an account. Its history is kept. */
    suspend fun archiveAccount(id: AccountId)
}
