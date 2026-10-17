package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.TransactionEntity
import java.util.Currency
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionMapperTest {

    private val lira = Currency.getInstance("TRY")

    private val expenseFromAccount = TransactionEntity(
        id = 9,
        type = "EXPENSE",
        amountMinor = 45_00,
        occurredAt = 1_000,
        accountId = 1,
        createdAt = 1_000,
    )

    @Test
    fun `an expense with both an account and a card fails and names the row`() {
        val error = assertThrows(IllegalStateException::class.java) {
            expenseFromAccount.copy(cardId = 2).asModel(lira)
        }

        assertTrue(error.message.orEmpty().contains("transaction 9"))
    }

    @Test
    fun `a column that belongs to another kind of transaction fails`() {
        assertThrows(IllegalStateException::class.java) {
            expenseFromAccount.copy(toAccountId = 2).asModel(lira)
        }
    }
}
