/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

/**
 * A resolved stream pair ready for playback (plan.md 1.6). The video source
 * is always a video-only stream and the audio source its DASH counterpart;
 * YT Desktop never seeks muxed streams.
 */
data class ResolvedPlayback(
    val video: PlaybackSource.Remote,
    val audio: PlaybackSource.Remote,
    val title: String,
    val uploaderName: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
)

/** Full metadata for a video page, including its related feed (1.4/1.5). */
data class StreamDetails(
    val title: String,
    val uploaderName: String,
    val uploaderUrl: String?,
    val durationSeconds: Long,
    val viewCount: Long,
    val description: String?,
    val thumbnailUrl: String?,
    val relatedVideos: List<StreamItem>,
)

data class ChannelTab(
    val name: String,
    val url: String,
)

/** Channel header plus its navigable tabs (plan.md 1.5 — seven playable tabs). */
data class ChannelPage(
    val title: String,
    val description: String?,
    val subscriberCount: Long?,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val tabs: List<ChannelTab>,
)