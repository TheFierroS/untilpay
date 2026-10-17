package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.AccountEntity
import io.github.thefierros.untilpay.core.model.Account
import io.github.thefierros.untilpay.core.model.AccountId
import io.github.thefierros.untilpay.core.model.AccountType
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountMapperTest {

    private fun row(name: String = "Ziraat", type: String = "BANK", currencyCode: String = "TRY") =
        AccountEntity(
            id = 7,
            name = name,
            type = type,
            currencyCode = currencyCode,
            includeInBudget = false,
            createdAt = 1_000,
        )

    @Test
    fun `a row becomes an account`() {
        val expected = Account(
            id = AccountId(7),
            name = "Ziraat",
            type = AccountType.BANK,
            currency = Currency.getInstance("TRY"),
            includeInBudget = false,
        )

        assertEquals(expected, row().asModel())
    }

    @Test
    fun `an unknown account type fails and names the row`() {
        val error = assertThrows(IllegalStateException::class.java) {
            row(type = "SAVINGS").asModel()
        }

        assertTrue(error.message.orEmpty().contains("account 7"))
    }

    @Test
    fun `an unknown currency fails`() {
        assertThrows(IllegalStateException::class.java) { row(currencyCode = "LIRA").asModel() }
    }

    @Test
    fun `a blank name fails`() {
        assertThrows(IllegalStateException::class.java) { row(name = " ").asModel() }
    }
}
