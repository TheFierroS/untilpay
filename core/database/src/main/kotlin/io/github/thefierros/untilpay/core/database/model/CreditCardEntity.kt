package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row in the `credit_cards` table.
 *
 * Describes the card itself; statements and installments live in their own
 * tables. Day values are checked by the core model when a row is converted,
 * because Room has no support for CHECK constraints.
 */
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** ISO 4217 currency code, such as TRY. */
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    /** Day of the month on which a statement is issued, from 1 to 31. */
    @ColumnInfo(name = "statement_day")
    val statementDay: Int,
    /** Day of the month on which the statement payment is due, from 1 to 31. */
    @ColumnInfo(name = "due_day")
    val dueDay: Int,
    /** Creation time in epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    /** Archive time in epoch milliseconds, or null while the card is active. */
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,
)
