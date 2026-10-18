package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A cost paid on the same day every month. Unlike balances and statements it
 * belongs to no account, so it carries its own currency.
 */
@Entity(tableName = "fixed_costs")
data class FixedCostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Amount in minor units of [currencyCode]. */
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    /** ISO 4217 currency code, such as TRY. */
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    /** Day of the month it is paid, from 1 to 31. */
    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int,
    /** Creation time in epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    /** Archive time in epoch milliseconds, or null while the cost is active. */
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,
)
