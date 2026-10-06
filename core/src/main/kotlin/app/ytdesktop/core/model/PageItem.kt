/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

/**
 * A row in any browsable list, normalised across the three NewPipeExtractor
 * info item kinds. Video rows are directly playable; playlist/channel rows
 * are navigable and become playable once opened.
 */
sealed interface PageItem {
    val title: String
    val url: String
    val thumbnailUrl: String?

    data class Video(val item: StreamItem) : PageItem {
        override val title: String get() = item.title
        override val url: String get() = item.url
        override val thumbnailUrl: String? get() = item.thumbnailUrl
    }

    data class Playlist(
        override val title: String,
        override val url: String,
        override val thumbnailUrl: String?,
        val uploaderName: String,
        val streamCount: Long,
    ) : PageItem

    data class Channel(
        override val title: String,
        override val url: String,
        override val thumbnailUrl: String?,
        val subscriberCount: Long?,
    ) : PageItem
}