package io.github.thefierros.untilpay.core.data.mapper

import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import io.github.thefierros.untilpay.core.model.Category
import io.github.thefierros.untilpay.core.model.CategoryId

internal fun CategoryEntity.asModel(): Category = fromRow("category $id") {
    Category(id = CategoryId(id), name = name)
}
