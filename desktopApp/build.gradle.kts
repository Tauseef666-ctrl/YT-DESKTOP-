/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    alias(libs.plugins.jetbrains.kotlin.compose)
    alias(libs.plugins.jetbrains.compose.multiplatform)
    alias(libs.plugins.about.libraries)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.jetbrains.coroutines.swing)
    implementation(libs.jetbrains.compose.preview)

    // Keep the material3 artifacts aligned with the shared module
    implementation(libs.jetbrains.compose.material3)
}

compose.desktop {
    application {
        mainClass = "net.newpipe.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "YT-Desktop"
            packageVersion = "0.29.5"
            description = "YT Desktop - Libre lightweight streaming client"
            copyright = "\u00a9 2026 NewPipe e.V. Licensed under GPL-3.0-or-later"
            vendor = "YT Desktop"

            windows {
                menu = true
                shortcut = true
                iconFile.set(project.file("src/main/resources/app-icon.ico"))
                dirChooser = true
                menuGroup = "YT Desktop"
                upgradeUuid = "6943C53C-2A58-48D3-87FD-4375A2BB5854"
                perUserInstall = false
            }
        }
    }
}

aboutLibraries {
    export {
        outputFile = file("../shared/src/jvmMain/resources/aboutlibraries.json")
        prettyPrint = true
        excludeFields.addAll("organization", "scm", "funding")
    }
}
