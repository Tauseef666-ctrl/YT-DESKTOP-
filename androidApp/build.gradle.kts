/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "app.ytdesktop.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.ytdesktop.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = providers.gradleProperty("yt.version").getOrElse("0.1.0")
    }

    // Two APKs from one code base. `phone` is the pure build; `tv` is the pure
    // build plus Android TV packaging (leanback launcher, banner, D-pad shell).
    // Manifest overlay + banner live in src/tv/, everything else is shared.
    flavorDimensions += "device"
    productFlavors {
        create("phone") {
            dimension = "device"
            isDefault = true
        }
        create("tv") {
            dimension = "device"
        }
    }

    buildTypes {
        release {
            // R8 is deliberately still off: it needs a ProGuard ruleset for
            // NewPipeExtractor + kotlinx.serialization that we cannot exercise
            // until an Android SDK is available locally. Tracked in plan.md.
            isMinifyEnabled = false
            // Signed with the debug keystore so the CI artifact is installable.
            // Replace with a real release keystore before publishing.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
}