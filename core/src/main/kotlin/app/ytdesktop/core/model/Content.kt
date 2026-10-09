/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

/**
 * A resolved stream ready for playback (plan.md 1.6).
 *
 * Normal videos pair a video-only stream with its synchronized DASH audio
 * counterpart ([audio] non-null) — YT Desktop never seeks muxed streams. Live
 * broadcasts cannot be split that way: YouTube serves a self-contained HLS (or
 * DASH) manifest carrying audio AND video, so for live the whole manifest goes
 * in [video] and [audio] is `null`.
 */
data class ResolvedPlayback(
    val video: PlaybackSource.Remote,
    val audio: PlaybackSource.Remote?,
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

/**
 * The streams a download should capture (plan.md 2.4). YouTube VODs are
 * video-only + audio-only DASH streams: each is fetched as its own file (the
 * Phase 2.3 muxers then combine them into one media file). Live broadcasts
 * cannot be downloaded — resolution rejects them.
 */
data class ResolvedDownload(
    val title: String,
    /** May be null for audio-only content. */
    val video: DownloadableStream?,
    val audio: DownloadableStream,
    val durationSeconds: Long,
)

/** One URL to save, with the container suffix its mime type implies. */
data class DownloadableStream(
    val url: String,
    /** "mp4" / "webm" / "m4a"… used for the destination file name. */
    val extension: String,
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