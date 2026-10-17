package io.github.thefierros.untilpay.core.model

/** Identifies an account. A distinct type so it can't be mixed up with other ids. */
@JvmInline
value class AccountId(val value: Long)

/** Identifies a credit card. */
@JvmInline
value class CardId(val value: Long)

/** Identifies a recurring source of income, such as a monthly allowance. */
@JvmInline
value class IncomeSourceId(val value: Long)

/** Identifies a card statement. */
@JvmInline
value class StatementId(val value: Long)

/** Identifies a spending category. */
@JvmInline
value class CategoryId(val value: Long)
