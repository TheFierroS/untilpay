package io.github.thefierros.untilpay.core.model

import org.junit.Assert.assertThrows
import org.junit.Test

class CategoryTest {

    @Test
    fun `a blank name is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { Category(CategoryId(1), " ") }
    }
}
