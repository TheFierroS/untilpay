package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring income paid into an account.
 *
 * The schedule is stored as a type plus one column per kind of schedule; only
 * the column that matches [scheduleType] is filled. The expected amount is in
 * the currency of the account it is paid into.
 */
@Entity(
    tableName = "income_sources",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["account_id"])],
)
data class IncomeSourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Expected amount in minor units of the account's currency. */
    @ColumnInfo(name = "expected_amount_minor")
    val expectedAmountMinor: Long,
    /** The account the income is paid into. */
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    /** MONTHLY, WEEKLY or ONCE. */
    @ColumnInfo(name = "schedule_type")
    val scheduleType: String,
    /** For MONTHLY: day of the month, from 1 to 31. */
    @ColumnInfo(name = "schedule_day_of_month")
    val scheduleDayOfMonth: Int? = null,
    /** For WEEKLY: ISO day of the week, from 1 (Monday) to 7 (Sunday). */
    @ColumnInfo(name = "schedule_day_of_week")
    val scheduleDayOfWeek: Int? = null,
    /** For ONCE: the date, as an epoch day. */
    @ColumnInfo(name = "schedule_date")
    val scheduleDate: Long? = null,
    /** Creation time in epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    /** Archive time in epoch milliseconds, or null while the income is active. */
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,
)
