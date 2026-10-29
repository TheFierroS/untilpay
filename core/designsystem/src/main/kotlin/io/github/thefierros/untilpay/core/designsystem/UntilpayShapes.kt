package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The outlines, hard shadows and corners that make the style neo-brutalist. */
@Immutable
data class UntilpayShapes(
    /** Thickness of every outline. */
    val border: Dp,
    /** How far hard shadows fall, to the right and down. */
    val shadowOffset: Dp,
    /** Shadow offset while a button is pressed, so it seems to sink. */
    val pressedShadowOffset: Dp,
    /** Corners of cards and sheets. */
    val cornerLarge: Dp,
    /** Corners of buttons, chips and boxes inside cards. */
    val cornerSmall: Dp,
)

/** The shape scales Untilpay knows. */
object UntilpayShapeScale {
    val default = UntilpayShapes(
        border = 3.dp,
        shadowOffset = 6.dp,
        pressedShadowOffset = 2.dp,
        cornerLarge = 12.dp,
        cornerSmall = 8.dp,
    )
}
