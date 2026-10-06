// Only JVM + Compose plugins are declared here.
//
// AGP and the Kotlin Android plugin are referenced solely inside
// androidApp/build.gradle.kts, so a desktop-only build never resolves anything
// from dl.google.com (slow or blocked on some networks).
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
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
