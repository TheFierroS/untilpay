package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * A button with a thick outline and a hard shadow. When pressed it slides
 * toward its shadow, which shrinks, so the button seems to sink.
 * The label is uppercased with the rules of the current language.
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = UntilpayTheme.colors.primary,
) {
    val colors = UntilpayTheme.colors
    val shapes = UntilpayTheme.shapes
    val shape = RoundedCornerShape(shapes.cornerSmall)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shadow = if (pressed) shapes.pressedShadowOffset else shapes.shadowOffset
    val slide = shapes.shadowOffset - shadow
    val locale = LocalConfiguration.current.locales[0]

    Box(
        modifier = modifier
            .padding(end = shapes.shadowOffset, bottom = shapes.shadowOffset)
            .offset(x = slide, y = slide)
            .hardShadow(colors.ink, shape, shadow)
            .background(color, shape)
            .border(shapes.border, colors.ink, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = text.toDisplayText(locale),
            style = UntilpayTheme.typography.button.copy(color = colors.ink),
        )
    }
}

@Preview(locale = "tr")
@Composable
private fun NeoButtonPreview() {
    UntilpayTheme {
        Box(Modifier.background(UntilpayTheme.colors.backgroundCyan).padding(16.dp)) {
            NeoButton(text = "harcama ekle", onClick = {})
        }
    }
}
