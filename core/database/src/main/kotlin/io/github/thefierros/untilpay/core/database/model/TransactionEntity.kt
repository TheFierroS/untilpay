package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A movement of money: an expense, an income, a transfer between accounts or a
 * payment toward a card statement.
 *
 * Which reference columns are filled depends on [type]:
 * - EXPENSE: [accountId] or [cardId], optionally [categoryId]; card expenses
 *   paid in installments also set [installmentCount].
 * - INCOME: [accountId], optionally [incomeSourceId].
 * - TRANSFER: [accountId] (from) and [toAccountId] (to).
 * - CARD_PAYMENT: [accountId] (paid from) and [cardStatementId].
 *
 * The amount is always positive; the type gives the direction. Room has no
 * CHECK constraints, so these rules are enforced by the data layer.
 */
@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CardStatementEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_statement_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = IncomeSourceEntity::class,
            parentColumns = ["id"],
            childColumns = ["income_source_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["occurred_at"]),
        Index(value = ["account_id", "occurred_at"]),
        Index(value = ["to_account_id"]),
        Index(value = ["card_id", "occurred_at"]),
        Index(value = ["card_statement_id"]),
        Index(value = ["category_id"]),
        Index(value = ["income_source_id"]),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** EXPENSE, INCOME, TRANSFER or CARD_PAYMENT. */
    val type: String,
    /** Always positive, in minor units of the currency of the account or card. */
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    /** When the money moved, in epoch milliseconds. */
    @ColumnInfo(name = "occurred_at")
    val occurredAt: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long? = null,
    @ColumnInfo(name = "to_account_id")
    val toAccountId: Long? = null,
    @ColumnInfo(name = "card_id")
    val cardId: Long? = null,
    @ColumnInfo(name = "card_statement_id")
    val cardStatementId: Long? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: Long? = null,
    @ColumnInfo(name = "income_source_id")
    val incomeSourceId: Long? = null,
    /** For card expenses paid in installments: how many. Null for a single payment. */
    @ColumnInfo(name = "installment_count")
    val installmentCount: Int? = null,
    val note: String? = null,
    /** When the entry was made, in epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)
