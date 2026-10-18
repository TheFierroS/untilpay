package io.github.thefierros.untilpay.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.thefierros.untilpay.core.model.BudgetSettings
import io.github.thefierros.untilpay.core.model.Money
import java.io.IOException
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Stores the budget currency and the protected amount on the device. */
class BudgetSettingsDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {

    /** The current settings; Turkish lira and nothing protected until the user changes them. */
    val settings: Flow<BudgetSettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            val currency = Currency.getInstance(preferences[CURRENCY] ?: DEFAULT_CURRENCY)
            BudgetSettings(currency, Money(preferences[PROTECTED_MINOR] ?: 0, currency))
        }

    /**
     * Changes the budget currency. A protected amount saved in the old currency
     * would be misread in the new one, so it is cleared.
     */
    suspend fun setCurrency(currency: Currency) {
        dataStore.edit { preferences ->
            if ((preferences[CURRENCY] ?: DEFAULT_CURRENCY) != currency.currencyCode) {
                preferences[CURRENCY] = currency.currencyCode
                preferences[PROTECTED_MINOR] = 0
            }
        }
    }

    /** Saves the protected amount; it must be in the budget currency and not negative. */
    suspend fun setProtectedAmount(amount: Money) {
        dataStore.edit { preferences ->
            val currency = preferences[CURRENCY] ?: DEFAULT_CURRENCY
            require(amount.currency.currencyCode == currency) {
                "The protected amount must be in $currency, got ${amount.currency}"
            }
            require(amount.minorUnits >= 0) { "The protected amount cannot be negative" }
            preferences[PROTECTED_MINOR] = amount.minorUnits
        }
    }

    private companion object {
        const val DEFAULT_CURRENCY = "TRY"
        val CURRENCY = stringPreferencesKey("budget_currency")
        val PROTECTED_MINOR = longPreferencesKey("protected_amount_minor")
    }
}
