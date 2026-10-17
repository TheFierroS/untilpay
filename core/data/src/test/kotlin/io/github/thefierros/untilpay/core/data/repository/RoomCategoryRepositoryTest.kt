package io.github.thefierros.untilpay.core.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.model.Category
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCategoryRepositoryTest {

    private val now = Instant.parse("2026-10-18T09:00:00Z")

    private lateinit var database: UntilpayDatabase
    private lateinit var repository: RoomCategoryRepository

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        repository = RoomCategoryRepository(
            categoryDao = database.categoryDao(),
            clock = Clock.fixed(now, ZoneOffset.UTC),
        )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? =
        runCatching { block() }.exceptionOrNull()

    @Test
    fun `created categories are listed in creation order`() = runTest {
        val food = repository.createCategory("Food")
        val transport = repository.createCategory("Transport")

        assertEquals(
            listOf(Category(food, "Food"), Category(transport, "Transport")),
            repository.observeCategories().first(),
        )
    }

    @Test
    fun `a blank name is rejected before anything is saved`() = runTest {
        val error = failureOf { repository.createCategory(" ") }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(emptyList<Category>(), repository.observeCategories().first())
    }

    @Test
    fun `a renamed category is listed under its new name`() = runTest {
        val id = repository.createCategory("Food")

        repository.renameCategory(id, "Groceries")

        assertEquals(listOf(Category(id, "Groceries")), repository.observeCategories().first())
    }

    @Test
    fun `renaming to a blank name is rejected`() = runTest {
        val id = repository.createCategory("Food")

        val error = failureOf { repository.renameCategory(id, " ") }

        assertTrue("Expected a rejection but got $error", error is IllegalArgumentException)
        assertEquals(listOf(Category(id, "Food")), repository.observeCategories().first())
    }

    @Test
    fun `an archived category is no longer listed but is kept`() = runTest {
        val id = repository.createCategory("Food")

        repository.archiveCategory(id)

        assertEquals(emptyList<Category>(), repository.observeCategories().first())
        assertEquals(now.toEpochMilli(), database.categoryDao().findById(id.value)?.archivedAt)
    }
}
