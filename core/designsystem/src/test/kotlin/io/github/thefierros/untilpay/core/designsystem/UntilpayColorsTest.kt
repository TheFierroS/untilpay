package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UntilpayColorsTest {
    private val colors = UntilpayPalette.karnaval

    @Test
    fun `black text is readable on every karnaval color`() {
        val fills = mapOf(
            "surface" to colors.surface,
            "primary" to colors.primary,
            "statusCalm" to colors.statusCalm,
            "statusWarning" to colors.statusWarning,
            "statusOver" to colors.statusOver,
            "backgroundViolet" to colors.backgroundViolet,
            "backgroundCyan" to colors.backgroundCyan,
            "backgroundPink" to colors.backgroundPink,
            "backgroundYellow" to colors.backgroundYellow,
        )
        for ((name, fill) in fills) {
            val ratio = contrast(colors.ink, fill)
            assertTrue("$name gives only $ratio:1 with ink", ratio >= 4.5)
        }
    }

    @Test
    fun `the contrast formula gives 21 to 1 for black on white`() {
        assertEquals(21.0, contrast(Color.Black, Color.White), 0.01)
    }

    /** WCAG 2 relative luminance. */
    private fun luminance(color: Color): Double {
        fun channel(value: Float): Double {
            val v = value.toDouble()
            return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) +
            0.0722 * channel(color.blue)
    }

    /** WCAG 2 contrast ratio, from 1:1 to 21:1. */
    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
