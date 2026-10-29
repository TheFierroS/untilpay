package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Mutually exclusive choices side by side in one outlined box.
 * The chosen one is filled with the primary color.
 *
 * Labels use the body font and are shown exactly as given, so names like
 * "English" are never uppercased with another language's rules.
 */
@Composable
fun NeoChoiceRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = UntilpayTheme.colors
    val type = UntilpayTheme.typography
    val shapes = UntilpayTheme.shapes
    val shape = RoundedCornerShape(shapes.cornerSmall)

    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .border(shapes.border, colors.ink, shape)
            .clip(shape)
            .selectableGroup(),
    ) {
        options.forEachIndexed { index, label ->
            if (index > 0) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(shapes.border)
                        .background(colors.ink),
                )
            }
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (selected) colors.primary else colors.surface)
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    )
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = label,
                    style = (if (selected) type.bodyStrong else type.body).copy(color = colors.ink),
                )
            }
        }
    }
}

@Preview
@Composable
private fun NeoChoiceRowPreview() {
    UntilpayTheme {
        Box(Modifier.background(UntilpayTheme.colors.backgroundViolet).padding(16.dp)) {
            NeoChoiceRow(
                options = listOf("System", "English", "Türkçe"),
                selectedIndex = 2,
                onSelect = {},
            )
        }
    }
}
