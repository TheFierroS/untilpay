package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalUntilpayColors = staticCompositionLocalOf<UntilpayColors> {
    error("No UntilpayColors found; wrap the content in UntilpayTheme")
}

private val LocalUntilpayTypography = staticCompositionLocalOf<UntilpayTypography> {
    error("No UntilpayTypography found; wrap the content in UntilpayTheme")
}

private val LocalUntilpayShapes = staticCompositionLocalOf<UntilpayShapes> {
    error("No UntilpayShapes found; wrap the content in UntilpayTheme")
}

/** Gives everything inside it the Untilpay colors, text styles and shapes. */
@Composable
fun UntilpayTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalUntilpayColors provides UntilpayPalette.karnaval,
        LocalUntilpayTypography provides UntilpayTypeScale.default,
        LocalUntilpayShapes provides UntilpayShapeScale.default,
        content = content,
    )
}

/** Reads the current theme: `UntilpayTheme.colors.primary` and so on. */
object UntilpayTheme {
    val colors: UntilpayColors
        @Composable
        @ReadOnlyComposable
        get() = LocalUntilpayColors.current

    val typography: UntilpayTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalUntilpayTypography.current

    val shapes: UntilpayShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalUntilpayShapes.current
}
