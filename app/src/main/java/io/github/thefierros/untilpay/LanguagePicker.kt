package io.github.thefierros.untilpay

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.os.LocaleListCompat
import io.github.thefierros.untilpay.core.designsystem.NeoChoiceRow

/**
 * Lets the user pick the app language.
 *
 * The choice is stored by the system on Android 13+ and by AppCompat below that;
 * the app never stores it itself. Picking a language recreates the activity,
 * so the whole screen redraws in the new language.
 */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val languages = AppLanguage.entries
    val current = AppLanguage.fromTags(AppCompatDelegate.getApplicationLocales().toLanguageTags())
    NeoChoiceRow(
        options = languages.map { it.label() },
        selectedIndex = languages.indexOf(current),
        onSelect = { index ->
            AppCompatDelegate.setApplicationLocales(languages[index].toLocaleList())
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
