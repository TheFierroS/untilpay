package io.github.thefierros.untilpay

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import io.github.thefierros.untilpay.core.designsystem.NeoButton
import io.github.thefierros.untilpay.core.designsystem.NeoCard
import io.github.thefierros.untilpay.core.designsystem.UntilpayTheme
import io.github.thefierros.untilpay.core.designsystem.toDisplayText

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Untilpay is always light, so the system bar icons stay dark.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            UntilpayTheme {
                DesignSystemDemo()
            }
        }
    }
}

/**
 * Temporary screen that shows the design system on a phone.
 * The real screens replace it in #8 and #9.
 */
@Composable
private fun DesignSystemDemo() {
    val colors = UntilpayTheme.colors
    val type = UntilpayTheme.typography
    val shapes = UntilpayTheme.shapes
    val locale = LocalConfiguration.current.locales[0]
    val boxShape = RoundedCornerShape(shapes.cornerSmall)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundViolet)
            .safeDrawingPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // A brand name keeps its own spelling: no locale casing.
        BasicText(text = "UNTILPAY", style = type.headline)
        NeoCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BasicText(
                    text = "bugün harcayabileceğin".toDisplayText(locale),
                    style = type.title,
                )
                Box(
                    modifier = Modifier
                        .background(colors.primary, boxShape)
                        .border(shapes.border, colors.ink, boxShape)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    BasicText(text = "42 ₺", style = type.number)
                }
                BasicText(text = "Sonraki gelire 25 gün", style = type.body)
            }
        }
        NeoButton(
            text = "harcama ekle",
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(locale = "tr")
@Composable
private fun DesignSystemDemoPreview() {
    UntilpayTheme {
        DesignSystemDemo()
    }
}
