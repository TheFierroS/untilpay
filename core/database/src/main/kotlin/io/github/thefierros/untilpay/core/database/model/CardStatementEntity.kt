package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A statement issued for a credit card, as printed by the bank, plus what the
 * user plans to pay toward it.
 *
 * Payments made and spending since the statement are not stored here; they are
 * derived from transactions so each fact is recorded in exactly one place.
 * Dates are calendar days counted from 1970-01-01 (LocalDate.toEpochDay), not
 * moments in time, so they never shift with the time zone.
 */
@Entity(
    tableName = "card_statements",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["card_id", "closing_date"], unique = true)],
)
data class CardStatementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "card_id")
    val cardId: Long,
    /** The day the statement was cut, as an epoch day. */
    @ColumnInfo(name = "closing_date")
    val closingDate: Long,
    /** The payment due date printed on the statement, as an epoch day. */
    @ColumnInfo(name = "due_date")
    val dueDate: Long,
    /** Debt shown on the statement, in minor units of the card's currency. */
    @ColumnInfo(name = "balance_minor")
    val balanceMinor: Long,
    /** Minimum payment shown on the statement, in minor units. */
    @ColumnInfo(name = "minimum_payment_minor")
    val minimumPaymentMinor: Long,
    /** What the user plans to pay toward the statement, in minor units. */
    @ColumnInfo(name = "planned_payment_minor")
    val plannedPaymentMinor: Long,
)
