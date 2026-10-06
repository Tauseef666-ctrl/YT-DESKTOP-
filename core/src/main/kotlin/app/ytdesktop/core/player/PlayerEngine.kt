/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.player

import app.ytdesktop.core.model.PlaybackSource

/**
 * The player seam (plan.md 1.6). Core owns the contract and the queue logic;
 * desktopApp supplies the libVLC implementation and Android will supply a
 * Media3 one in a future phase.
 */
interface PlayerEngine {

    /** Latest observable state snapshot (atomic snapshot, never mutated in place). */
    val state: PlayerState

    /** Begins playback of a single [source]; [title] is for the now-playing UI. */
    fun play(source: PlaybackSource, audioOnly: Boolean = false, title: String? = null)

    fun togglePlayPause()

    fun stop()

    /** Seek to a position, as a fraction 0..1 of the media length. */
    fun seekTo(fraction: Float)

    /** Jump [seconds] from the current position (negative jumps back). */
    fun skip(seconds: Long)

    /** Releases native media resources; the engine must not be used afterwards. */
    fun release()

    fun setListener(listener: Listener?)

    fun interface Listener {
        fun onStateChanged(newState: PlayerState)
    }
}

/** Immutable snapshot of engine state, queried by UI for recomposition. */
data class PlayerState(
    val isPlaying: Boolean = false,
    val isAudioOnly: Boolean = false,
    val timeMs: Long = 0L,
    val lengthMs: Long = 0L,
    val positionFraction: Float = 0f,
    val currentTitle: String? = null,
    val error: String? = null,
) {
    val hasMedia: Boolean get() = lengthMs > 0L
}