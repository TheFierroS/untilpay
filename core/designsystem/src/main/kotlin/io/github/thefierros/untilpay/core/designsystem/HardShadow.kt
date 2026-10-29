package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * Draws a hard, blur-free copy of [shape] in [color], shifted right and down by [offset].
 * The caller must leave room for it, for example with end and bottom padding.
 */
internal fun Modifier.hardShadow(color: Color, shape: Shape, offset: Dp): Modifier = drawBehind {
    val outline = shape.createOutline(size, layoutDirection, this)
    val px = offset.toPx()
    translate(left = px, top = px) {
        drawOutline(outline, color)
    }
}
