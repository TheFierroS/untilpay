package io.github.thefierros.untilpay.core.model

/** A spending category, such as food or transport. */
data class Category(val id: CategoryId, val name: String) {
    init {
        require(name.isNotBlank()) { "Category name must not be blank" }
    }
}
