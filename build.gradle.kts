// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
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

// Spotless gets its files from a fileTree so that excluded directories are
// pruned while walking. A plain targetExclude would still walk into build
// outputs that other tasks are writing to at the same time, which fails
// randomly when tasks run in parallel.
fun sourceFiles(pattern: String) = fileTree(rootDir) {
    include(pattern)
    exclude("**/build/**", "**/.gradle/**", "**/.kotlin/**", "**/.idea/**")
}

spotless {
    kotlin {
        target(sourceFiles("**/*.kt"))
        ktlint()
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(ktlintRules)
    }
    kotlinGradle {
        target(sourceFiles("**/*.gradle.kts"))
        ktlint()
            .setEditorConfigPath(rootProject.file(".editorconfig"))
            .editorConfigOverride(ktlintRules)
    }
}
