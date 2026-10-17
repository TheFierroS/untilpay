plugins {
    alias(libs.plugins.untilpay.jvm.library)
}

dependencies {
    api(project(":core:model"))
}
