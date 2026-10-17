import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "io.github.thefierros.untilpay.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = libs.plugins.untilpay.android.application.get().pluginId
            implementationClass =
                "io.github.thefierros.untilpay.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = libs.plugins.untilpay.android.library.get().pluginId
            implementationClass =
                "io.github.thefierros.untilpay.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = libs.plugins.untilpay.jvm.library.get().pluginId
            implementationClass =
                "io.github.thefierros.untilpay.buildlogic.JvmLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = libs.plugins.untilpay.android.hilt.get().pluginId
            implementationClass =
                "io.github.thefierros.untilpay.buildlogic.HiltConventionPlugin"
        }
    }
}
