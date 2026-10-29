plugins {
    alias(libs.plugins.untilpay.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.thefierros.untilpay.core.designsystem"

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
