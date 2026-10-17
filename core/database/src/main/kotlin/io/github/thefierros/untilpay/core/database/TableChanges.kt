package io.github.thefierros.untilpay.core.database

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Tells readers when tables change, so they can read again. */
class TableChanges @Inject constructor(private val database: UntilpayDatabase) {

    /**
     * Emits once right away and again after every committed change to any of
     * [tables]. Throws if one of the tables does not exist.
     */
    fun observe(vararg tables: String): Flow<Unit> =
        database.invalidationTracker.createFlow(*tables).map { }
}
