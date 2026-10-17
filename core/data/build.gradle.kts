plugins {
    alias(libs.plugins.untilpay.android.library)
    alias(libs.plugins.untilpay.android.hilt)
}

android {
    namespace = "io.github.thefierros.untilpay.core.data"
}

dependencies {
    api(project(":core:model"))
    api(project(":core:domain"))
    implementation(project(":core:database"))

    testImplementation(libs.junit)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
}
