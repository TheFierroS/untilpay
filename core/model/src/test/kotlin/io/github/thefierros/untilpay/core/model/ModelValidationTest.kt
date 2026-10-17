package io.github.thefierros.untilpay.core.model

import java.util.Currency
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelValidationTest {

    private val lira = Currency.getInstance("TRY")

    private fun account(name: String = "Ziraat") =
        Account(AccountId(1), name, AccountType.BANK, lira, includeInBudget = true)

    private fun card(name: String = "Bankkart", statementDay: Int = 5, dueDay: Int = 15) =
        CreditCard(CardId(1), name, lira, statementDay, dueDay)

    private fun incomeSource(
        name: String = "Allowance",
        expectedAmount: Money = Money(800000, lira),
    ) = IncomeSource(
        IncomeSourceId(1),
        name,
        expectedAmount,
        IncomeSchedule.Monthly(dayOfMonth = 1),
        AccountId(1),
    )

    @Test
    fun `valid models can be created`() {
        account()
        card()
        incomeSource()
    }

    @Test
    fun `blank names are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { account(name = " ") }
        assertThrows(IllegalArgumentException::class.java) { card(name = "") }
        assertThrows(IllegalArgumentException::class.java) { incomeSource(name = "  ") }
    }

    @Test
    fun `card days outside 1 to 31 are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { card(statementDay = 0) }
        assertThrows(IllegalArgumentException::class.java) { card(dueDay = 32) }
    }

    @Test
    fun `an income source must expect a positive amount`() {
        assertThrows(IllegalArgumentException::class.java) {
            incomeSource(expectedAmount = Money.zero(lira))
        }
        assertThrows(IllegalArgumentException::class.java) {
            incomeSource(expectedAmount = Money(-100, lira))
        }
    }
}
