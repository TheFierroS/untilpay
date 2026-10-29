package io.github.thefierros.untilpay

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import io.github.thefierros.untilpay.core.designsystem.NeoButton
import io.github.thefierros.untilpay.core.designsystem.NeoCard
import io.github.thefierros.untilpay.core.designsystem.NeoChoiceRow
import io.github.thefierros.untilpay.core.designsystem.UntilpayTheme
import io.github.thefierros.untilpay.core.designsystem.toDisplayText
import java.util.Locale

/**
 * A small button showing the current language code, such as "TR".
 * Tapping it opens a dialog with the [LanguagePicker].
 */
@Composable
fun LanguageButton(modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    // A language code is not a word of any language, so no locale casing.
    val code = locale.language.uppercase(Locale.ROOT)

    NeoButton(
        text = code,
        onClick = { open = true },
        modifier = modifier,
        color = UntilpayTheme.colors.surface,
    )

    if (open) {
        Dialog(onDismissRequest = { open = false }) {
            NeoCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BasicText(
                        text = stringResource(R.string.language_title).toDisplayText(locale),
                        style = UntilpayTheme.typography.title,
                    )
                    LanguagePicker(onPicked = { open = false })
                }
            }
        }
    }
}

/**
 * Lets the user pick the app language.
 *
 * The choice is stored by the system on Android 13+ and by AppCompat below that;
 * the app never stores it itself. The activity handles locale changes, so the
 * screen redraws in the new language without being recreated.
 */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier, onPicked: () -> Unit = {}) {
    val languages = AppLanguage.entries
    val current = AppLanguage.fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    NeoChoiceRow(
        options = languages.map { it.label() },
        selectedIndex = languages.indexOf(current),
        onSelect = { index ->
            AppCompatDelegate.setApplicationLocales(languages[index].toLocaleList())
            onPicked()
        },
        modifier = modifier,
    )
}

@Composable
private fun AppLanguage.label(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
    AppLanguage.TURKISH -> stringResource(R.string.language_turkish)
}

private fun AppLanguage.toLocaleList(): LocaleListCompat =
    tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
