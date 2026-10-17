package io.github.thefierros.untilpay.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyTest {

    @Test
    fun `division rounds down to the nearest kurus`() {
        assertEquals(Money(333), Money(1000) / 3L)
    }

    @Test
    fun `division of a negative amount rounds toward negative infinity`() {
        assertEquals(Money(-334), Money(-1000) / 3L)
    }

    @Test
    fun `division by zero is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Money.lira(10) / 0L }
    }

    @Test
    fun `division by a negative number is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Money.lira(10) / -2L }
    }
}
