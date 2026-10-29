package io.github.thefierros.untilpay.core.designsystem

import java.util.Locale

/**
 * Prepares text for the display font, which draws every letter as a capital.
 *
 * The casing rules of the [locale] are used, so in Turkish "i" becomes "İ"
 * and "ı" becomes "I". A plain [String.uppercase] would turn both into "I".
 */
fun String.toDisplayText(locale: Locale): String = uppercase(locale)
