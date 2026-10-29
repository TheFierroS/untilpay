package io.github.thefierros.untilpay.core.designsystem

import org.junit.Assert.assertSame
import org.junit.Test

class UntilpayTypographyTest {
    private val type = UntilpayTypeScale.default

    @Test
    fun `number headline title and button use the display font`() {
        listOf(type.number, type.headline, type.title, type.button).forEach {
            assertSame(UntilpayFonts.display, it.fontFamily)
        }
    }

    @Test
    fun `body bodyStrong and label use the body font`() {
        listOf(type.body, type.bodyStrong, type.label).forEach {
            assertSame(UntilpayFonts.body, it.fontFamily)
        }
    }
}
