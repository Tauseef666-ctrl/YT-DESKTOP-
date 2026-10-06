/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

/**
 * One slot in the play queue: the provenance of the tapped video plus the
 * concrete source libVLC should open. Keeping both lets the queue advance to
 * "next" via the watch URL while staying on the exact stream libVLC supports.
 */
data class PlayQueueItem(
    val title: String,
    val uploaderName: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val watchUrl: String,
    val source: PlaybackSource,
)