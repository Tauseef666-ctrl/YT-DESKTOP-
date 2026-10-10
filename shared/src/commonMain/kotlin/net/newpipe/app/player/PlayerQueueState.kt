/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

/**
 * Snapshot of the queue for the UI, derived from [PlayQueue] so composables never mutate it.
 */
data class PlayerQueueState(
    val items: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF
) {
    val current: MediaItem? get() = items.getOrNull(currentIndex)
    val isEmpty: Boolean get() = items.isEmpty()
}
