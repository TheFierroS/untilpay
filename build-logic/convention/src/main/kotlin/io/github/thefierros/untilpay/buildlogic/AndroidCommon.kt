package io.github.thefierros.untilpay.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

/** Configuration shared by every Android module, whether application or library. */
internal fun configureAndroidCommon(android: CommonExtension) {
    android.compileSdk = AndroidSdk.COMPILE
    android.defaultConfig.minSdk = AndroidSdk.MIN
    android.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    android.compileOptions.targetCompatibility = JavaVersion.VERSION_17

    android.lint.apply {
        // Zero-warning policy: any lint warning fails the build.
        warningsAsErrors = true
        abortOnError = true

        // These checks depend on the current date rather than the code, so they
        // would turn CI red without any change on our side. Dependabot handles
        // dependency updates instead.
        disable += setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
            "OldTargetApi",
        )
    }
}
