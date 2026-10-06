// Only JVM + Compose plugins are applied here.
//
// AGP and kotlin-android are declared with `apply false` for a purely mechanical
// reason: Gradle gives the root plugins{} block and each subproject's plugins{}
// block their own classloader. If AGP is only declared in androidApp, KGP (loaded
// by the root via kotlin-jvm) cannot see AGP's classes and applying
// org.jetbrains.kotlin.android dies with NoClassDefFoundError BaseVariant.
// Declaring them at root puts everything on the same classpath. Nothing is
// *applied* here, so desktop-only builds still work without the Android SDK.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
}

allprojects {
    group = "app.ytdesktop"
    version = providers.gradleProperty("yt.version").getOrElse("0.1.0")
}

subprojects {
    // Toolchain is JDK 21, but emitted bytecode targets 17 on purpose: `core` is a
    // plain JVM jar that :androidApp consumes, and Android's D8/desugaring is
    // happiest with class files no newer than 17.
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
