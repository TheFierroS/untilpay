package io.github.thefierros.untilpay.core.data.repository

import io.github.thefierros.untilpay.core.data.mapper.asModel
import io.github.thefierros.untilpay.core.database.dao.CategoryDao
import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import io.github.thefierros.untilpay.core.model.Category
import io.github.thefierros.untilpay.core.model.CategoryId
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val clock: Clock,
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> =
        categoryDao.observeActive().map { rows -> rows.map { it.asModel() } }

    override suspend fun createCategory(name: String): CategoryId {
        // Building the model first applies its rules, such as a non-blank name.
        val draft = Category(CategoryId(0), name)
        return CategoryId(
            categoryDao.insert(CategoryEntity(name = draft.name, createdAt = clock.millis())),
        )
    }

    override suspend fun renameCategory(id: CategoryId, name: String) {
        val renamed = Category(id, name)
        categoryDao.update(existing(id).copy(name = renamed.name))
    }

    override suspend fun archiveCategory(id: CategoryId) {
        categoryDao.archive(existing(id).id, archivedAt = clock.millis())
    }

    private suspend fun existing(id: CategoryId): CategoryEntity =
        requireNotNull(categoryDao.findById(id.value)) { "There is no category ${id.value}" }
}
