/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ytdesktop.core.errors.ErrorInfo
import app.ytdesktop.core.errors.UserAction
import app.ytdesktop.core.errors.YtException
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.StreamingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The play queue (plan.md 1.7). Tapped videos go onto a contiguous queue;
 * playback moves forward automatically when the current media finishes, and
 * the queue UI can jump to any slot. Resolution happens here so the engine's
 * contract stays sync and single-purpose.
 */
class PlayQueueController(
    private val scope: CoroutineScope,
    private val service: StreamingService,
    private val engine: VlcPlayerEngine,
) {
    val items = mutableStateListOf<StreamItem>()

    var currentIndex by mutableStateOf(-1)
        private set

    /** True while a queued video is being resolved into playable streams. */
    var resolving by mutableStateOf(false)
        private set

    init {
        engine.onFinished = {
            if (currentIndex in items.indices && currentIndex < items.lastIndex) {
                playAt(currentIndex + 1)
            }
        }
    }

    /** Plays [item] alone, clearing any previous queue. */
    fun play(item: StreamItem) {
        items.clear()
        items.add(item)
        playAt(0)
    }

    fun playAt(index: Int) {
        if (index !in items.indices) return
        currentIndex = index
        resolveAndPlay(items[index])
    }

    fun next() {
        if (currentIndex < items.lastIndex) playAt(currentIndex + 1)
    }

    fun previous() {
        if (currentIndex > 0) playAt(currentIndex - 1)
    }

    fun clear() {
        items.clear()
        currentIndex = -1
        engine.stop()
    }

    private fun resolveAndPlay(item: StreamItem) {
        resolving = true
        scope.launch {
            try {
                val resolved = service.resolvePlayback(item.url)
                engine.play(
                    source = resolved.video,
                    title = resolved.title,
                    companionAudio = resolved.audio,
                )
            } catch (e: YtException) {
                engine.reportLoadFailure(item.title, e.errorInfo)
            } catch (e: Exception) {
                engine.reportLoadFailure(
                    item.title,
                    ErrorInfo(UserAction.STREAM_RESOLUTION, e.message ?: "Could not load this video"),
                )
            } finally {
                resolving = false
            }
        }
    }
}