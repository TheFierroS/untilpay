package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.CategoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: CategoryDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.categoryDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun category(name: String, createdAt: Long) =
        CategoryEntity(name = name, createdAt = createdAt)

    @Test
    fun `an inserted category can be read back with its generated id`() = runTest {
        val id = dao.insert(category("Food", createdAt = 1_000))

        assertEquals(category("Food", createdAt = 1_000).copy(id = id), dao.findById(id))
    }

    @Test
    fun `active categories are listed in creation order without archived ones`() = runTest {
        dao.insert(category("Transport", createdAt = 2_000))
        val unused = dao.insert(category("Unused", createdAt = 1_500))
        dao.insert(category("Food", createdAt = 1_000))

        dao.archive(unused, archivedAt = 3_000)

        val names = dao.observeActive().first().map { it.name }
        assertEquals(listOf("Food", "Transport"), names)
    }

    @Test
    fun `a renamed category is saved`() = runTest {
        val id = dao.insert(category("Food", createdAt = 1_000))
        val original = checkNotNull(dao.findById(id))

        dao.update(original.copy(name = "Groceries"))

        assertEquals("Groceries", dao.findById(id)?.name)
    }
}
