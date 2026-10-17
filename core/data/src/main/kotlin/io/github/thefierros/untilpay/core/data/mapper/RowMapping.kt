package io.github.thefierros.untilpay.core.data.mapper

import java.time.DateTimeException

/**
 * Runs [block], which turns a database row into a core model, and names the
 * row if that fails.
 *
 * Only this app writes the database, so a row that cannot become a valid model
 * means a bug stored it. Failing loudly is safer for a budget than quietly
 * skipping a money record or guessing what it meant.
 */
internal inline fun <T> fromRow(row: String, block: () -> T): T = try {
    block()
} catch (e: IllegalArgumentException) {
    throw IllegalStateException("Stored $row is invalid: ${e.message}", e)
} catch (e: DateTimeException) {
    throw IllegalStateException("Stored $row is invalid: ${e.message}", e)
}
