/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop

import java.io.File

/**
 * Points JNA and libVLC at the copy of VLC bundled with the packaged app.
 *
 * Must run **before** any VLCJ class is loaded, because the native library is
 * discovered during class initialisation.
 *
 * Layout produced by the `vlcSetup` plugin inside a packaged app:
 *   `<install dir>/resources/windows/vlc/{libvlc.dll, plugins/}`
 * When running from the IDE there is no packaged resources dir, so we fall back
 * to a system-installed VLC / whatever `vlc.plugin.path` already points at.
 */
object BundledVlc {

    private const val WINDOWS_VLC_SUBDIR = "windows/vlc"

    /** True when we successfully located the bundled native libraries. */
    var isBundled: Boolean = false
        private set

    fun configure() {
        val resourcesDir = System.getProperty("compose.application.resources.dir")
            ?.let(::File)
            ?.takeIf { it.isDirectory }
            ?: return

        val vlcDir = File(resourcesDir, WINDOWS_VLC_SUBDIR)
            .takeIf { it.isDirectory }
            ?: return

        System.setProperty("jna.library.path", vlcDir.absolutePath)
        System.setProperty(
            "vlc.plugin.path",
            File(vlcDir, "plugins").takeIf { it.isDirectory }?.absolutePath
                ?: File(resourcesDir, "windows/vlc-plugins").absolutePath,
        )
        isBundled = true
    }
}