package io.github.thefierros.untilpay.core.designsystem

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * The two typefaces of Untilpay, both under the SIL Open Font License.
 * The license texts live in `core/designsystem/licenses`.
 */
object UntilpayFonts {
    /**
     * Bungee Inline: headlines, the daily number and buttons.
     * It only has capital letters, so text must go through [toDisplayText] first.
     */
    val display: FontFamily = FontFamily(
        Font(R.font.bungee_inline_regular, FontWeight.Normal),
    )

    /** Space Grotesk: everything else. */
    val body: FontFamily = FontFamily(
        Font(R.font.space_grotesk_regular, FontWeight.Normal),
        Font(R.font.space_grotesk_medium, FontWeight.Medium),
        Font(R.font.space_grotesk_bold, FontWeight.Bold),
    )
}
