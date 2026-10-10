/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import kotlin.random.Random

/** How the queue advances past its end (and repeats). */
enum class RepeatMode { OFF, ALL, ONE }

/**
 * Ordered play queue with repeat and shuffle, independent of any engine so it can be tested and
 * persisted on every platform. All mutation keeps [current] stable where a reasonable choice
 * exists (e.g. removing an earlier item, or shuffling).
 */
class PlayQueue(
    initialItems: List<MediaItem> = emptyList(),
    repeatMode: RepeatMode = RepeatMode.OFF,
    private val random: Random = Random.Default
) {
    private val queue = initialItems.toMutableList()
    private var originalOrder: List<MediaItem>? = null

    var repeatMode: RepeatMode = repeatMode
        set(value) {
            field = value
        }

    var isShuffled: Boolean = false
        private set

    var currentIndex: Int = if (queue.isEmpty()) -1 else 0
        private set

    val items: List<MediaItem> get() = queue.toList()
    val size: Int get() = queue.size
    val isEmpty: Boolean get() = queue.isEmpty()
    val current: MediaItem? get() = queue.getOrNull(currentIndex)

    /** Replace the whole queue, starting at [startIndex] (clamped). Clears shuffle. */
    fun replaceAll(newItems: List<MediaItem>, startIndex: Int = 0) {
        queue.clear()
        queue.addAll(newItems)
        originalOrder = null
        isShuffled = false
        currentIndex = when {
            queue.isEmpty() -> -1
            startIndex in queue.indices -> startIndex
            else -> 0
        }
    }

    /**
     * Restore a persisted queue exactly: same display order (which already reflects shuffle),
     * [repeatMode], [currentIndex] (clamped) and shuffle flag. Does not re-shuffle.
     */
    fun restore(newItems: List<MediaItem>, current: Int, repeat: RepeatMode, shuffled: Boolean) {
        queue.clear()
        queue.addAll(newItems)
        originalOrder = null
        repeatMode = repeat
        isShuffled = shuffled
        currentIndex = when {
            queue.isEmpty() -> -1
            current in queue.indices -> current
            else -> 0
        }
    }

    /** Append to the end; becomes current if the queue was empty. */
    fun enqueue(item: MediaItem) {
        queue.add(item)
        if (currentIndex < 0) currentIndex = 0
    }

    /** Insert directly after the current item (or append when empty). */
    fun enqueueNext(item: MediaItem) {
        if (currentIndex < 0) {
            queue.add(item)
            currentIndex = 0
        } else {
            queue.add(currentIndex + 1, item)
        }
    }

    /** Jump to an index; returns the new current item or null when out of range. */
    fun jumpTo(index: Int): MediaItem? {
        if (index !in queue.indices) return null
        currentIndex = index
        return current
    }

    /** Remove by index, keeping [current] pointing at the same item when possible. */
    fun removeAt(index: Int): MediaItem? {
        if (index !in queue.indices) return null
        val removed = queue.removeAt(index)
        when {
            queue.isEmpty() -> currentIndex = -1
            index < currentIndex -> currentIndex--
            currentIndex > queue.lastIndex -> currentIndex = queue.lastIndex
        }
        return removed
    }

    fun remove(item: MediaItem): Boolean {
        val index = queue.indexOf(item)
        if (index < 0) return false
        removeAt(index)
        return true
    }

    /** Move an item; returns true when the move was valid. [current] is preserved by identity. */
    fun move(from: Int, to: Int): Boolean {
        if (from !in queue.indices || to !in queue.indices || from == to) return false
        val currentItem = current
        val moved = queue.removeAt(from)
        queue.add(to, moved)
        currentIndex = if (currentItem != null) queue.indexOf(currentItem) else currentIndex
        return true
    }

    fun clear() {
        queue.clear()
        originalOrder = null
        isShuffled = false
        currentIndex = -1
    }

    /** True when [next] would return a (possibly wrapped/repeated) item. */
    fun hasNext(): Boolean = when {
        isEmpty -> false
        repeatMode != RepeatMode.OFF -> true
        else -> currentIndex < queue.lastIndex
    }

    /** Advance per [repeatMode]; returns null when playback should stop. */
    fun next(): MediaItem? {
        if (isEmpty) return null
        return when {
            repeatMode == RepeatMode.ONE -> current
            currentIndex < queue.lastIndex -> {
                currentIndex++
                current
            }

            repeatMode == RepeatMode.ALL -> {
                currentIndex = 0
                current
            }

            else -> null
        }
    }

    /** Step back; wraps only under [RepeatMode.ALL]. */
    fun previous(): MediaItem? {
        if (isEmpty) return null
        return when {
            currentIndex > 0 -> {
                currentIndex--
                current
            }

            repeatMode == RepeatMode.ALL -> {
                currentIndex = queue.lastIndex
                current
            }

            else -> null
        }
    }

    /**
     * Toggle shuffle. Shuffling keeps [current] selected; un-shuffling restores the original
     * order and keeps [current] selected.
     */
    fun toggleShuffle() {
        val currentItem = current
        if (!isShuffled) {
            originalOrder = queue.toList()
            queue.shuffle(random)
            currentIndex = currentItem?.let { queue.indexOf(it) } ?: -1
            isShuffled = true
        } else {
            originalOrder?.let { restored ->
                queue.clear()
                queue.addAll(restored)
            }
            currentIndex = currentItem?.let { queue.indexOf(it) } ?: -1
            originalOrder = null
            isShuffled = false
        }
    }
}
