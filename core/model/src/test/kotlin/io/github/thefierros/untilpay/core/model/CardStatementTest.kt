package io.github.thefierros.untilpay.core.model

import java.time.LocalDate
import java.util.Currency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CardStatementTest {

    private val lira = Currency.getInstance("TRY")
    private val euro = Currency.getInstance("EUR")
    private val closing = LocalDate.of(2026, 10, 15)

    private fun tl(minorUnits: Long) = Money(minorUnits, lira)

    private fun statement(
        dueDate: LocalDate = closing.plusDays(10),
        balance: Money = tl(2_000_00),
        minimumPayment: Money = tl(400_00),
        plannedPayment: Money = tl(1_500_00),
    ) = CardStatement(
        id = StatementId(1),
        cardId = CardId(1),
        closingDate = closing,
        dueDate = dueDate,
        balance = balance,
        minimumPayment = minimumPayment,
        plannedPayment = plannedPayment,
    )

    @Test
    fun `a statement due on its closing day and planned below the minimum is valid`() {
        val statement = statement(dueDate = closing, plannedPayment = tl(100_00))

        assertEquals(closing, statement.dueDate)
    }

    @Test
    fun `a due date before the closing date is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            statement(dueDate = closing.minusDays(1))
        }
    }

    @Test
    fun `amounts in different currencies are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            statement(plannedPayment = Money(100_00, euro))
        }
    }

    @Test
    fun `a negative balance is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            statement(balance = tl(-1), minimumPayment = tl(0))
        }
    }

    @Test
    fun `a minimum payment above the balance is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            statement(balance = tl(300_00), minimumPayment = tl(400_00))
        }
    }

    @Test
    fun `a negative planned payment is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            statement(plannedPayment = tl(-1))
        }
    }
}
