package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.model.Category
import io.github.thefierros.untilpay.core.model.CategoryId
import kotlinx.coroutines.flow.Flow

/** Creates and changes the categories spending is sorted into. */
interface CategoryRepository {

    /** Active categories in the order they were created. */
    fun observeCategories(): Flow<List<Category>>

    /** Creates a category and returns its id. */
    suspend fun createCategory(name: String): CategoryId

    suspend fun renameCategory(id: CategoryId, name: String)

    /** Hides a category. Past spending keeps it. */
    suspend fun archiveCategory(id: CategoryId)
}
