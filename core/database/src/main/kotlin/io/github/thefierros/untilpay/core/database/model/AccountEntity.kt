package io.github.thefierros.untilpay.core.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A row in the `accounts` table.
 *
 * Mirrors the shape of the table rather than the domain: the account type is
 * stored by name and times as epoch milliseconds. Converting to and from core
 * models happens in the data layer.
 */
@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** The name of an account type, such as BANK. Never its ordinal. */
    val type: String,
    /** ISO 4217 currency code, such as TRY. */
    @ColumnInfo(name = "currency_code")
    val currencyCode: String,
    @ColumnInfo(name = "include_in_budget")
    val includeInBudget: Boolean,
    /** Creation time in epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    /** Archive time in epoch milliseconds, or null while the account is active. */
    @ColumnInfo(name = "archived_at")
    val archivedAt: Long? = null,
)
