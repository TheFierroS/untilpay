package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Text styles named by the job they do. The first four use the display font,
 * so their text must go through [toDisplayText]; the rest use the body font.
 */
@Immutable
data class UntilpayTypography(
    /** The daily number. */
    val number: TextStyle,
    /** Screen headlines. */
    val headline: TextStyle,
    /** Card titles. */
    val title: TextStyle,
    /** Button labels. */
    val button: TextStyle,
    /** Running text. */
    val body: TextStyle,
    /** Running text that needs weight. */
    val bodyStrong: TextStyle,
    /** Small labels and captions. */
    val label: TextStyle,
)

/** The type scales Untilpay knows. */
object UntilpayTypeScale {
    val default = UntilpayTypography(
        number = TextStyle(
            fontFamily = UntilpayFonts.display,
            fontSize = 56.sp,
            lineHeight = 60.sp,
        ),
        headline = TextStyle(
            fontFamily = UntilpayFonts.display,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        ),
        title = TextStyle(fontFamily = UntilpayFonts.display, fontSize = 18.sp, lineHeight = 22.sp),
        button = TextStyle(
            fontFamily = UntilpayFonts.display,
            fontSize = 16.sp,
            lineHeight = 20.sp,
        ),
        body = TextStyle(
            fontFamily = UntilpayFonts.body,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        bodyStrong = TextStyle(
            fontFamily = UntilpayFonts.body,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        ),
        label = TextStyle(
            fontFamily = UntilpayFonts.body,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
    )
}
