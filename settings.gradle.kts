rootProject.name = "yt-desktop"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// Pure JVM modules: no Android SDK required.
// `core` and `ui` are plain Kotlin/JVM, so the desktop build never touches AGP.
include(":core")
include(":ui")
include(":desktopApp")

// The Android app is only configured when explicitly requested, so a desktop-only
// build works on a machine with no Android SDK installed.
//   desktop only :  gradlew :desktopApp:packageMsi
//   with Android :  gradlew -Pyt.android=on :androidApp:assembleDebug
if (providers.gradleProperty("yt.android").orNull == "on") {
    include(":androidApp")
}
