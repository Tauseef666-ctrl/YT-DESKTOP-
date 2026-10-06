/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.time.Duration

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
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(libs.vlcj)
    implementation(libs.jna)
    implementation(libs.jna.platform)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.okhttp)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit5)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Spike S3 plays real media through libVLC in-process, so the staged native
// libraries must be on disk before the test JVM starts and discoverable by JNA.
val stagedVlcOsDir = run {
    val os = System.getProperty("os.name")?.lowercase() ?: ""
    when {
        os.contains("win") -> "windows"
        os.contains("mac") -> "macos"
        else -> "linux"
    }
}
val stagedVlcHome = layout.projectDirectory.dir("src/main/appResources/$stagedVlcOsDir/vlc")

tasks.test {
    dependsOn("vlcSetup")
    systemProperty("yt.vlcHome", stagedVlcHome.asFile.absolutePath)
    systemProperty("jna.library.path", stagedVlcHome.asFile.absolutePath)
    systemProperty("vlc.plugin.path", stagedVlcHome.dir("plugins").asFile.absolutePath)
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    // Remote DASH extraction plus two playback round-trips.
    timeout.set(Duration.ofMinutes(5))
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
    // Skip UPX during spikes: compressing ~1k plugin DLLs costs minutes per CI
    // cycle for no functional gain here. Re-enable before shipping the installer.
    shouldCompressVlcFiles = false
    // MUST stay true. The plugin's built-in keep-list ships only ~20 DLLs and
    // omits everything YT Desktop needs: access/libhttp+libhttps (any network
    // playback), demux/libadaptive (VLC 3 puts HLS *and* DASH there),
    // demux/libmp4, demux/libwebm, and the whole spu/ tree (subtitles).
    // Spike S3 failed against the filtered set for exactly this reason.
    // Revisit only with a keep-list we have verified end to end.
    shouldIncludeAllVlcFiles = true
    pathToCopyVlcLinuxFilesTo = file("src/main/appResources/linux/vlc")
    pathToCopyVlcMacosFilesTo = file("src/main/appResources/macos/vlc")
    pathToCopyVlcWindowsFilesTo = file("src/main/appResources/windows/vlc")
}

// get.videolan.org intermittently truncates the ~77 MB VLC archive
// ("Premature end of Content-Length delimited message body ... expected:
// 77665682; received: 130789"), which failed an otherwise green Windows job.
// The plugin's Download tasks default to 0 retries, so harden them all.
tasks.withType<de.undercouch.gradle.tasks.download.Download>().configureEach {
    retries(5)
    connectTimeout(30_000)
    readTimeout(5 * 60_000)
    // Stage to a temp file and rename only on success, so a partial download
    // cannot poison a retry.
    tempAndMove(true)
}