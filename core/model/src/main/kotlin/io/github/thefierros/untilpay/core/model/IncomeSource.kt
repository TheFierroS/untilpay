package io.github.thefierros.untilpay.core.model

/** A recurring income, such as a monthly allowance, paid into an account. */
data class IncomeSource(
    val id: IncomeSourceId,
    val name: String,
    val expectedAmount: Money,
    val schedule: IncomeSchedule,
    /** The account the income is paid into. */
    val accountId: AccountId,
) {
    init {
        require(name.isNotBlank()) { "Income source name must not be blank" }
        require(expectedAmount.isPositive) {
            "expectedAmount must be positive, got $expectedAmount"
        }
    }
}
