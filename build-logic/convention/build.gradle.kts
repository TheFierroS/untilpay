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
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
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
