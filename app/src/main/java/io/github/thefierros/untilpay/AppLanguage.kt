package io.github.thefierros.untilpay

/**
 * The languages a user can pick inside the app.
 * [tag] is the language tag, or null to follow the system language.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    TURKISH("tr"),
    ;

    companion object {
        /**
         * Finds the language from AppCompat's language tags, such as "tr-TR" or "en-US,tr".
         * An empty list means the app follows the system; an unknown language is treated the same way.
         */
        fun fromTags(tags: String): AppLanguage {
            val first = tags.substringBefore(',').substringBefore('-')
            return entries.firstOrNull { it.tag == first } ?: SYSTEM
        }
    }
}
