import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

compose.resources {
    packageOfResClass = "com.rentman.sharedui.resources"
    generateResClass = always
}

kotlin {
    android {
        namespace = "com.rentman.sharedui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget = JvmTarget.JVM_17 }
        androidResources { enable = true }
    }
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "sharedUI"
            isStatic = true
        }
    }
    jvm() // tests only

    compilerOptions { allWarningsAsErrors = true }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain"))
            implementation(project(":data")) // DI wiring only (ARCHITECTURE.md §2)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(libs.androidx.navigation3.ui)
            implementation(libs.androidx.savedstate)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
        }
        androidMain.dependencies {
            implementation(libs.androidx.work.runtime) // reminders, ARCHITECTURE.md section 7
            implementation(libs.androidx.activity.compose) // the notification permission prompt
            implementation(libs.mlkit.text.recognition) // reads the meter photo on the phone, bundled so it works offline
            implementation(libs.androidx.exifinterface) // turns a photo the right way up
        }
        // Test-only JVM target: the JDK's NumberFormat can't group digits the Indian way (₹1,24,000), ICU can.
        jvmMain.dependencies {
            implementation(libs.icu4j)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
