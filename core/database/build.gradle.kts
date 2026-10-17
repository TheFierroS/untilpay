plugins {
    alias(libs.plugins.untilpay.android.library)
    alias(libs.plugins.untilpay.android.room)
    alias(libs.plugins.untilpay.android.hilt)
}

android {
    namespace = "io.github.thefierros.untilpay.core.database"
}

dependencies {
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
