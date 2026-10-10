/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.newpipe.app.player.MediaItem
import net.newpipe.app.player.PlaybackController
import net.newpipe.app.player.PlaybackState
import net.newpipe.app.player.PlayerQueueState
import net.newpipe.app.player.PlayerShortcuts
import net.newpipe.app.player.Quality
import net.newpipe.app.player.RepeatMode
import net.newpipe.app.preferences.VideoAudioPreferences
import org.koin.core.annotation.KoinViewModel

/**
 * Thin screen-facing wrapper over the shared [PlaybackController]. Queue and engine live in the
 * controller so playback survives navigation between the player and other screens.
 */
@KoinViewModel
class PlayerViewModel(
    private val playback: PlaybackController,
    private val settings: Settings
) : ViewModel() {

    val isEngineAvailable: Boolean = playback.isEngineAvailable
    val state: StateFlow<PlaybackState> = playback.state
    val queueState: StateFlow<PlayerQueueState> = playback.queueState

    /** Milliseconds per seek (F2): the configured `seek_duration`, Shift = large skip. */
    fun seekStepMillis(isShiftPressed: Boolean): Long {
        val configuredMs = settings.getString(
            VideoAudioPreferences.KEY_SEEK_DURATION,
            VideoAudioPreferences.DEFAULT_SEEK_DURATION_MS
        ).toLongOrNull() ?: PlayerShortcuts.DEFAULT_SEEK_STEP_MS
        return PlayerShortcuts.seekStepMillis(isShiftPressed, configuredMs)
    }

    fun play() = playback.play()
    fun pause() = playback.pause()
    fun stop() = playback.stop()
    fun togglePlayPause() = playback.togglePlayPause()
    fun seekTo(positionMs: Long) = playback.seekTo(positionMs)
    fun setSpeed(speed: Float) = playback.setSpeed(speed)
    fun setVolume(volume: Float) = playback.setVolume(volume)
    fun setMuted(muted: Boolean) = playback.setMuted(muted)
    fun toggleMute() = playback.toggleMute()
    fun setQuality(quality: Quality) = playback.setQuality(quality)

    fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        viewModelScope.launch { playback.setQueue(items, startIndex) }
    }

    fun playItem(item: MediaItem) {
        viewModelScope.launch { playback.playItem(item) }
    }

    fun next() {
        viewModelScope.launch { playback.next() }
    }

    fun previous() {
        viewModelScope.launch { playback.previous() }
    }

    fun toggleShuffle() = playback.toggleShuffle()

    fun setRepeatMode(mode: RepeatMode) = playback.setRepeatMode(mode)
}
