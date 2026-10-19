package io.github.thefierros.untilpay.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.thefierros.untilpay.core.model.BudgetSettings
import io.github.thefierros.untilpay.core.model.Money
import java.io.File
import java.util.Currency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BudgetSettingsDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")

    private fun TestScope.dataSource() = BudgetSettingsDataSource(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(folder.root, "settings.preferences_pb") },
        ),
    )

    @Test
    fun `without saved settings the budget is in lira with nothing protected`() = runTest {
        assertEquals(BudgetSettings(lira, Money.zero(lira)), dataSource().settings.first())
    }

    @Test
    fun `a protected amount is saved`() = runTest {
        val source = dataSource()

        source.setProtectedAmount(Money(500_00, lira))

        assertEquals(Money(500_00, lira), source.settings.first().protectedAmount)
    }

    @Test
    fun `changing the currency clears the protected amount`() = runTest {
        val source = dataSource()
        source.setProtectedAmount(Money(500_00, lira))

        source.setCurrency(euro)

        assertEquals(BudgetSettings(euro, Money.zero(euro)), source.settings.first())
    }

    @Test
    fun `choosing the same currency again keeps the protected amount`() = runTest {
        val source = dataSource()
        source.setProtectedAmount(Money(500_00, lira))

        source.setCurrency(lira)

        assertEquals(Money(500_00, lira), source.settings.first().protectedAmount)
    }

    @Test
    fun `a protected amount in another currency is rejected and nothing is saved`() = runTest {
        val source = dataSource()

        val error = runCatching { source.setProtectedAmount(Money(100, euro)) }.exceptionOrNull()

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(Money.zero(lira), source.settings.first().protectedAmount)
    }
}
