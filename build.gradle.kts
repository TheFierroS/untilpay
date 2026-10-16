// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}
// Passed explicitly because Spotless does not forward these .editorconfig
// properties to ktlint. Keep them in sync with .editorconfig, which
// Android Studio reads for its own formatter.
val ktlintRules =
    mapOf(
        "ktlint_code_style" to "android_studio",
        "ij_kotlin_allow_trailing_comma" to "true",
        "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
    )

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint()
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(ktlintRules)
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(ktlintRules)
    }
}
