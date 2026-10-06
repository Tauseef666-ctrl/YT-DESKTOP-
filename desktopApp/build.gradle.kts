/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.vlc.setup)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))

    implementation(compose.desktop.currentOs)
    implementation(libs.vlcj)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.okhttp)
}

compose.desktop {
    application {
        mainClass = "app.ytdesktop.MainKt"

        // VLCJ reaches into java.nio internals for its ByteBuffer factory.
        jvmArgs += "--add-opens=java.base/java.nio=ALL-UNNAMED"
        jvmArgs += "-Xmx2g"

        nativeDistributions {
            // Must match the directory passed to vlcSetup{ pathToCopyVlc*FilesTo }.
            appResourcesRootDir = project.layout.projectDirectory.dir("src/main/appResources")

            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "YTDesktop"
            packageVersion = project.version.toString()
            description = "A YouTube front-end for Windows and Android"
            vendor = "YT Desktop contributors"

            modules(
                "java.instrument",
                "java.management",
                "java.prefs",
                "java.sql",
                "jdk.unsupported",
            )

            windows {
                menu = true
                // Stable so jpackage can upgrade an existing install in place.
                upgradeUuid = "6F1A2C34-9B8D-4E7F-A1B2-C3D4E5F60718"
            }
        }
    }
}

// Downloads the pinned libVLC build and stages its shared libraries + plugins
// under src/main/appResources so the packaged app needs no system VLC install.
vlcSetup {
    vlcVersion = libs.versions.vlc.get()
    shouldCompressVlcFiles = true
    // false keeps the download small; the base set plays the formats we need.
    // Set to true if codec coverage turns out to be insufficient.
    shouldIncludeAllVlcFiles = false
    pathToCopyVlcLinuxFilesTo = file("src/main/appResources/linux/vlc")
    pathToCopyVlcMacosFilesTo = file("src/main/appResources/macos/vlc")
    pathToCopyVlcWindowsFilesTo = file("src/main/appResources/windows/vlc")
}

// get.videolan.org is frequently slow and the plugin's Download tasks default to
// 0 retries / a short read timeout, which makes packaging flaky on CI.
tasks.withType<de.undercouch.gradle.tasks.download.Download>().configureEach {
    retries(4)
    connectTimeout(30_000)
    readTimeout(5 * 60_000)
    tempAndMove(true)
}