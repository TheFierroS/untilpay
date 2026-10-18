package io.github.thefierros.untilpay.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.thefierros.untilpay.core.database.UntilpayDatabase
import io.github.thefierros.untilpay.core.database.model.FixedCostEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FixedCostDaoTest {

    private lateinit var database: UntilpayDatabase
    private lateinit var dao: FixedCostDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            UntilpayDatabase::class.java,
        ).build()
        dao = database.fixedCostDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    private fun cost(name: String, dayOfMonth: Int) = FixedCostEntity(
        name = name,
        amountMinor = 500_00,
        currencyCode = "TRY",
        dayOfMonth = dayOfMonth,
        createdAt = 1_000,
    )

    @Test
    fun `an inserted fixed cost can be read back with its generated id`() = runTest {
        val id = dao.insert(cost("Phone", dayOfMonth = 15))

        assertEquals(cost("Phone", dayOfMonth = 15).copy(id = id), dao.findById(id))
    }

    @Test
    fun `active fixed costs are listed by payment day without archived ones`() = runTest {
        dao.insert(cost("Phone", dayOfMonth = 15))
        val cancelled = dao.insert(cost("Gym", dayOfMonth = 5))
        dao.insert(cost("Dormitory", dayOfMonth = 1))

        dao.archive(cancelled, archivedAt = 2_000)

        val names = dao.observeActive().first().map { it.name }
        assertEquals(listOf("Dormitory", "Phone"), names)
    }

    @Test
    fun `changes to a fixed cost are saved`() = runTest {
        val id = dao.insert(cost("Phone", dayOfMonth = 15))
        val original = checkNotNull(dao.findById(id))

        dao.update(original.copy(amountMinor = 650_00, dayOfMonth = 20))

        val updated = checkNotNull(dao.findById(id))
        assertEquals(650_00L, updated.amountMinor)
        assertEquals(20, updated.dayOfMonth)
    }
}
