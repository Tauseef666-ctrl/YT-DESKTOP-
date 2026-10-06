/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import app.ytdesktop.core.errors.ErrorInfo
import app.ytdesktop.core.model.PlaybackSource
import app.ytdesktop.core.player.PlayerEngine
import app.ytdesktop.core.player.PlayerState

/**
 * The libVLC [PlayerEngine] (plan.md 1.6). Desktop's thin adapter over
 * [VlcPlayer]: it flattens the player's snapshot fields into an immutable
 * [PlayerState] for the chrome and routes transport commands verbatim.
 *
 * DASH pair playback (S1 finding): resolving a video gives a video-only
 * stream plus its synchronized audio-only partner. The engine plays the video
 * MRL as the master input and hands the audio URL to libVLC as `input-slave`
 * (with the `#audio#` designation), so VLC muxes them on one clock.
 *
 * The engine is created after `BundledVlc.configure()` only, matching the
 * lifecycle rules of [VlcPlayer].
 */
class VlcPlayerEngine(private val player: VlcPlayer) : PlayerEngine {

    private var titleOverride: String? = null
    private var errorOverride: String? = null
    private var isAudioOnly = false

    /** Auto-advance hook fired when the current media finishes. */
    var onFinished: (() -> Unit)? = null

    init {
        player.onFinished = { onFinished?.invoke() }
    }

    override val state: PlayerState
        get() = PlayerState(
            isPlaying = player.isPlaying,
            isAudioOnly = isAudioOnly,
            timeMs = player.timeMs,
            lengthMs = player.lengthMs,
            positionFraction = player.position,
            currentTitle = titleOverride,
            error = errorOverride ?: player.error,
        )

    override fun play(
        source: PlaybackSource,
        audioOnly: Boolean,
        title: String?,
        companionAudio: PlaybackSource?,
    ) {
        titleOverride = title
        errorOverride = null
        isAudioOnly = audioOnly

        if (audioOnly) {
            player.play(source.mrl())
        } else if (source is PlaybackSource.Remote && companionAudio is PlaybackSource.Remote) {
            player.play(source.mrl(), options = listOf(":input-slave=#audio#${companionAudio.url}"))
        } else {
            player.play(source.mrl())
        }
    }

    override fun togglePlayPause() = player.togglePlayPause()

    override fun stop() = player.stop()

    override fun seekTo(fraction: Float) = player.seekTo(fraction)

    override fun skip(seconds: Long) = player.skipSeconds(seconds)

    override fun release() = player.release()

    /** Surfaces a failure that happened before [play] (e.g. stream resolution). */
    fun reportLoadFailure(what: String, info: ErrorInfo) {
        titleOverride = what
        errorOverride = info.message
    }

    /**
     * The UI observes [state] through Compose snapshots on a stable instance,
     * so listeners are never needed; kept as a no-op for interface parity.
     */
    override fun setListener(listener: PlayerEngine.Listener?) = Unit
}