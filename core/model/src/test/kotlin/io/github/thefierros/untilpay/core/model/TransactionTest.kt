package io.github.thefierros.untilpay.core.model

import java.time.Instant
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TransactionTest {

    private val lira = Currency.getInstance("TRY")
    private val now = Instant.parse("2026-10-18T09:00:00Z")
    private val bank = AccountId(1)

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    private fun expense(amount: Money = tl(45_00), note: String? = null) = Transaction.Expense(
        id = TransactionId(1),
        amount = amount,
        occurredAt = now,
        source = PaymentSource.FromAccount(bank),
        note = note,
    )

    @Test
    fun `a zero amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { expense(amount = tl(0)) }
    }

    @Test
    fun `a negative amount is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Transaction.Income(TransactionId(1), tl(-1), now, accountId = bank)
        }
    }

    @Test
    fun `a blank note is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { expense(note = " ") }
    }

    @Test
    fun `a transfer to the same account is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            Transaction.Transfer(TransactionId(1), tl(100_00), now, bank, toAccountId = bank)
        }
    }

    @Test
    fun `installments run from 2 to the limit`() {
        val card = CardId(1)

        assertEquals(2, PaymentSource.OnCard(card, installments = 2).installments)
        assertEquals(60, PaymentSource.OnCard(card, installments = 60).installments)
    }

    @Test
    fun `one installment or more than the limit is rejected`() {
        val card = CardId(1)

        assertThrows(IllegalArgumentException::class.java) {
            PaymentSource.OnCard(card, installments = 1)
        }
        assertThrows(IllegalArgumentException::class.java) {
            PaymentSource.OnCard(card, installments = 61)
        }
    }
}
