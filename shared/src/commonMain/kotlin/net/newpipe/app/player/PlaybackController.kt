/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.newpipe.app.preferences.PlayerPreferences
import org.koin.core.annotation.Singleton

/**
 * Single shared owner of the play [PlayQueue] and the [PlayerEngine]. Any screen (player, library,
 * downloads) can enqueue and control playback through this one instance, so the queue survives
 * navigation. The engine is a singleton and is intentionally never released here.
 *
 * Also owns smart playback resume (F9): positions are persisted via [PlaybackResumeStore] and
 * replayed as [MediaItem.resumePositionMs] whenever an item is loaded.
 *
 * The play queue itself is persisted via [PlayQueueStore] (F6): the last queue (order, current
 * item, repeat, shuffle) survives a restart without auto-playing anything.
 */
@Singleton
class PlaybackController(
    private val engine: PlayerEngine,
    private val resumeStore: PlaybackResumeStore,
    private val queueStore: PlayQueueStore,
    private val settings: Settings
) {

    val isEngineAvailable: Boolean = engine.isAvailable
    val state: StateFlow<PlaybackState> = engine.state

    private val resumeEnabled: Boolean
        get() = settings.getBoolean(
            PlayerPreferences.KEY_RESUME_PLAYBACK,
            PlayerPreferences.DEFAULT_RESUME_PLAYBACK
        )

    private val queue = PlayQueue()

    private val callScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val _queueState = MutableStateFlow(PlayerQueueState())
    val queueState: StateFlow<PlayerQueueState> = _queueState.asStateFlow()

    init {
        queueStore.load()?.let { restored ->
            queue.restore(
                newItems = restored.items,
                current = restored.currentIndex,
                repeat = restored.repeatMode,
                shuffled = restored.isShuffled
            )
        }
        publishQueue()
        // Persist positions as playback progresses — especially whenever an item is paused or ends.
        watchEngineForResumeSaves()
    }

    fun play() = engine.play()
    fun pause() = engine.pause()
    fun stop() = engine.stop()
    fun togglePlayPause() = engine.togglePlayPause()
    fun seekTo(positionMs: Long) = engine.seekTo(positionMs)
    fun setSpeed(speed: Float) = engine.setSpeed(speed)
    fun setVolume(volume: Float) = engine.setVolume(volume)
    fun setMuted(muted: Boolean) = engine.setMuted(muted)
    fun toggleMute() = engine.setMuted(!state.value.isMuted)
    fun setQuality(quality: Quality) = engine.setQuality(quality)

    /** Replace the whole queue and start the item at [startIndex]. */
    suspend fun setQueue(items: List<MediaItem>, startIndex: Int = 0) {
        queue.replaceAll(items, startIndex)
        publishQueue()
        queue.current?.let { prepareResumed(it) }
    }

    /** Play [item]: jump to it if already queued, otherwise enqueue and select it. */
    suspend fun playItem(item: MediaItem) {
        val existing = queue.items.indexOf(item)
        val target = if (existing >= 0) {
            existing
        } else {
            queue.enqueue(item)
            queue.items.lastIndex
        }
        queue.jumpTo(target)
        publishQueue()
        prepareResumed(queue.current ?: item)
    }

    suspend fun next() {
        queue.next()?.let { prepareResumed(it) }
        publishQueue()
    }

    suspend fun previous() {
        queue.previous()?.let { prepareResumed(it) }
        publishQueue()
    }

    /** Non-suspending next/previous for callers without their own scope (e.g. the mini player). */
    fun skipToNext() = callScope.launch { next() }

    fun skipToPrevious() = callScope.launch { previous() }

    fun toggleShuffle() {
        queue.toggleShuffle()
        publishQueue()
    }

    fun setRepeatMode(mode: RepeatMode) {
        queue.repeatMode = mode
        publishQueue()
    }

    /** Empty the queue (F6). Stops nothing already playing; it just has no next up. */
    fun clearQueue() {
        queue.clear()
        publishQueue()
    }

    /** Explicitly persist the current position (used by periodic/exit-point observers). */
    fun saveCurrentPosition() {
        val current = state.value
        val id = current.mediaId ?: return
        if (current.positionMs > 0L) {
            resumeStore.save(id, current.positionMs, current.durationMs)
        }
    }

    /** Forget any stored resume position for [mediaId] (privacy/cleanup). */
    fun clearResumePosition(mediaId: String) = resumeStore.clear(mediaId)

    /** Forget every stored resume position (privacy/clear-history). */
    fun clearResumePositions() = resumeStore.clearAll()

    /** Enables/disables smart resume for the whole app (reuses [PlayerPreferences.KEY_RESUME_PLAYBACK]). */
    fun setResumeEnabled(enabled: Boolean) {
        settings.putBoolean(PlayerPreferences.KEY_RESUME_PLAYBACK, enabled)
    }

    private suspend fun prepareResumed(item: MediaItem) {
        val resumePosition = resumePositionFor(item)
        val prepared = if (resumePosition > 0L) item.copy(resumePositionMs = resumePosition) else item
        engine.prepare(prepared)
    }

    private fun resumePositionFor(item: MediaItem): Long {
        if (!resumeEnabled) return 0L
        if (item.isLive) return 0L
        if (item.durationMs in 1 until PlayerPreferences.SHORT_CLIP_MAX_MS) {
            resumeStore.clear(item.id)
            return 0L
        }
        return resumeStore.load(item.id)
    }

    private fun watchEngineForResumeSaves() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        var periodicId: String? = null
        var periodicAt = 0L
        scope.launch {
            engine.state.collect { current ->
                val id = current.mediaId ?: return@collect
                when (current.status) {
                    PlaybackStatus.PAUSED, PlaybackStatus.ENDED, PlaybackStatus.READY -> {
                        if (current.positionMs > 0L) {
                            resumeStore.save(id, current.positionMs, current.durationMs)
                            periodicId = id
                            periodicAt = System.currentTimeMillis()
                        }
                    }
                    PlaybackStatus.PLAYING -> {
                        val now = System.currentTimeMillis()
                        val due = periodicId != id || now - periodicAt >= SAVE_INTERVAL_MS
                        if (current.positionMs > 0L && due) {
                            resumeStore.save(id, current.positionMs, current.durationMs)
                            periodicId = id
                            periodicAt = now
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun publishQueue() {
        _queueState.value = PlayerQueueState(
            items = queue.items,
            currentIndex = queue.currentIndex,
            isShuffled = queue.isShuffled,
            repeatMode = queue.repeatMode
        )
        persistQueue()
    }

    private fun persistQueue() {
        if (queue.isEmpty) {
            queueStore.clear()
        } else {
            queueStore.save(
                PlayQueueSnapshot(
                    items = queue.items,
                    currentIndex = queue.currentIndex,
                    repeatMode = queue.repeatMode,
                    isShuffled = queue.isShuffled
                )
            )
        }
    }

    private companion object {
        const val SAVE_INTERVAL_MS = 5_000L
    }
}