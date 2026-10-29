package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors named by the job they do, not by how they look.
 * Screens never use a raw [Color]; they ask the theme for a role.
 */
@Immutable
data class UntilpayColors(
    /** Text, outlines and hard shadows. */
    val ink: Color,
    /** Cards and sheets. */
    val surface: Color,
    /** The daily number box and the main highlights. */
    val primary: Color,
    /** The budget is comfortable. */
    val statusCalm: Color,
    /** The budget is getting tight. */
    val statusWarning: Color,
    /** The budget is over. */
    val statusOver: Color,
    /** Screen backgrounds; every screen picks one. */
    val backgroundViolet: Color,
    val backgroundCyan: Color,
    val backgroundPink: Color,
    val backgroundYellow: Color,
)

/** The palettes Untilpay knows. */
object UntilpayPalette {
    /** Karnaval, decided on 29 October 2026. */
    val karnaval = UntilpayColors(
        ink = Color(0xFF000000),
        surface = Color(0xFFFFFFFF),
        primary = Color(0xFFFFF503),
        statusCalm = Color(0xFF00FF75),
        statusWarning = Color(0xFFFFB443),
        statusOver = Color(0xFFFF5D5D),
        backgroundViolet = Color(0xFFDD7DFF),
        backgroundCyan = Color(0xFF38DBFF),
        backgroundPink = Color(0xFFFF3EA5),
        backgroundYellow = Color(0xFFFFF503),
    )
}
