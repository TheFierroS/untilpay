package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.CreditCardEntity
import io.github.thefierros.untilpay.core.model.CardId
import io.github.thefierros.untilpay.core.model.CreditCard
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CreditCardMapperTest {

    private fun row(statementDay: Int = 15) = CreditCardEntity(
        id = 2,
        name = "Bankkart",
        currencyCode = "TRY",
        statementDay = statementDay,
        dueDay = 25,
        createdAt = 1_000,
    )

    @Test
    fun `a row becomes a credit card`() {
        val expected = CreditCard(
            id = CardId(2),
            name = "Bankkart",
            currency = Currency.getInstance("TRY"),
            statementDay = 15,
            dueDay = 25,
        )

        assertEquals(expected, row().asModel())
    }

    @Test
    fun `a statement day outside the month fails`() {
        assertThrows(IllegalStateException::class.java) { row(statementDay = 32).asModel() }
    }
}
