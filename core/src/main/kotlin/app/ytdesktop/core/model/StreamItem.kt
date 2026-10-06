/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

import org.schabi.newpipe.extractor.stream.StreamInfoItem

/**
 * The display model for any playable row in a column: search hit, feed card,
 * related video, channel video tab. Deliberately UI-agnostic so both the
 * desktop app and Android can render it.
 */
data class StreamItem(
    val title: String,
    val uploaderName: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val viewCount: Long,
    val url: String,
    val shortDescription: String? = null,
) {
    fun watchUrl(): String = url

    companion object {
        fun fromNewPipe(item: StreamInfoItem): StreamItem = StreamItem(
            title = item.name,
            uploaderName = item.uploaderName,
            thumbnailUrl = item.thumbnails?.firstOrNull()?.url?.takeIf { it.isNotBlank() },
            durationSeconds = item.duration,
            viewCount = item.viewCount,
            url = item.url,
            shortDescription = item.shortDescription?.takeIf { it.isNotBlank() },
        )
    }
}