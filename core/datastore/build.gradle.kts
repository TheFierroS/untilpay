plugins {
    alias(libs.plugins.untilpay.android.library)
    alias(libs.plugins.untilpay.android.hilt)
}

android {
    namespace = "io.github.thefierros.untilpay.core.datastore"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
