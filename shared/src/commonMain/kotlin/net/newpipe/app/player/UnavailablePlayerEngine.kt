/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Honest no-op engine: reports [PlaybackStatus.ERROR] with a [PlaybackError.Unsupported] and never
 * pretends to play. Platforms that have not ported a real engine extend this so the shared UI and
 * Koin graph still resolve, while the unavailable state is surfaced to the user.
 */
open class UnavailablePlayerEngine(
    message: String = "Playback is not available on this platform yet."
) : PlayerEngine {

    override val isAvailable: Boolean = false

    private val _state = MutableStateFlow(
        PlaybackState(
            status = PlaybackStatus.ERROR,
            error = PlaybackError.Unsupported(message)
        )
    )

    override val state: StateFlow<PlaybackState> = _state

    override suspend fun prepare(item: MediaItem) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun stop() = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun setSpeed(speed: Float) = Unit

    override fun setVolume(volume: Float) = Unit

    override fun setMuted(muted: Boolean) = Unit

    override fun setQuality(quality: Quality) = Unit

    override fun release() = Unit
}
