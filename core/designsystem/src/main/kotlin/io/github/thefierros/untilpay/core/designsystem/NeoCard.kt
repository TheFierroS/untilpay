package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

/** A card with a thick outline and a hard shadow. */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    color: Color = UntilpayTheme.colors.surface,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = UntilpayTheme.colors
    val shapes = UntilpayTheme.shapes
    val shape = RoundedCornerShape(shapes.cornerLarge)
    Box(
        modifier = modifier
            .padding(end = shapes.shadowOffset, bottom = shapes.shadowOffset)
            .hardShadow(colors.ink, shape, shapes.shadowOffset)
            .background(color, shape)
            .border(shapes.border, colors.ink, shape)
            .padding(16.dp),
        content = content,
    )
}

@Preview(locale = "tr")
@Composable
private fun NeoCardPreview() {
    UntilpayTheme {
        Box(Modifier.background(UntilpayTheme.colors.backgroundViolet).padding(16.dp)) {
            NeoCard {
                BasicText(
                    text = "bugün harcayabileceğin".toDisplayText(Locale.forLanguageTag("tr")),
                    style = UntilpayTheme.typography.title,
                )
            }
        }
    }
}
