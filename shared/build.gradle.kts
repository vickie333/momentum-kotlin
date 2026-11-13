plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kmp)
}

kotlin {
    android {
        namespace = "com.momentum.shared"
        compileSdk = 37
        minSdk = 26
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()
    jvmToolchain(21)

    sourceSets {
        commonMain.dependencies {
            implementation(libs.serialization.json)
            implementation(libs.datetime)
            implementation(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
