/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

/**
 * The seam between what the user pressed play on and what libVLC actually
 * opens (see plan.md 1.6). Remote sources come from googlevideo stream keys;
 * local sources come from downloaded files or the bundled test assets.
 */
sealed interface PlaybackSource {
    /** Short human description, e.g. "720p (vp9)" used in the queue UI. */
    val label: String

    /** Absolute media locator handed to libVLC. */
    fun mrl(): String

    data class Remote(
        val url: String,
        val resolution: String? = null,
        val codec: String? = null,
        val itag: Int? = null,
        val mimeType: String? = null,
    ) : PlaybackSource {
        override val label: String
            get() = listOf(resolution, codec).filterNotNull().joinToString(" ") { it }

        override fun mrl(): String = url
    }

    data class Local(
        val path: String,
        val subtitleFile: String? = null,
    ) : PlaybackSource {
        override val label: String = path.substringAfterLast('/').substringAfterLast('\\')

        override fun mrl(): String = path
    }
}