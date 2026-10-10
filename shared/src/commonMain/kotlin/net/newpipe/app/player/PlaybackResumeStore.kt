/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import com.russhwolf.settings.Settings
import net.newpipe.app.preferences.PlayerPreferences
import org.koin.core.annotation.Singleton

/**
 * Persists per-media playback positions so a paused/stopped item can resume on the next load.
 *
 * Honest rules: nothing is stored for unknown durations or blank ids; a position is dropped as soon
 * as it reaches (or nearly reaches) the end, so finished media is never "resumed".
 */
@Singleton
class PlaybackResumeStore(
    private val settings: Settings
) {

    fun save(mediaId: String, positionMs: Long, durationMs: Long) {
        if (mediaId.isBlank()) return
        if (positionMs <= 0L) return
        if (durationMs > 0L && positionMs >= durationMs - NEAR_END_MS) {
            clear(mediaId)
            return
        }
        settings.putLong(positionKey(mediaId), positionMs)
        if (durationMs > 0L) settings.putLong(durationKey(mediaId), durationMs)
    }

    /** Returns the stored position (ms) or 0 when there is none / it is invalid. */
    fun load(mediaId: String): Long {
        if (mediaId.isBlank()) return 0L
        val position = settings.getLong(positionKey(mediaId), 0L)
        if (position <= 0L) return 0L
        val duration = settings.getLong(durationKey(mediaId), 0L)
        if (duration > 0L && position >= duration - NEAR_END_MS) {
            clear(mediaId)
            return 0L
        }
        return position
    }

    fun clear(mediaId: String) {
        settings.remove(positionKey(mediaId))
        settings.remove(durationKey(mediaId))
    }

    /** Remove every stored resume position (privacy/clear-history). */
    fun clearAll() {
        settings.keys
            .filter { it.startsWith(PlayerPreferences.PREFIX_RESUME_POSITION) || it.startsWith(PlayerPreferences.PREFIX_RESUME_DURATION) }
            .forEach { settings.remove(it) }
    }

    /** Number of media items that currently have a stored resume position. */
    fun count(): Int = settings.keys.count { it.startsWith(PlayerPreferences.PREFIX_RESUME_POSITION) }

    private fun positionKey(mediaId: String): String =
        PlayerPreferences.PREFIX_RESUME_POSITION + mediaId

    private fun durationKey(mediaId: String): String =
        PlayerPreferences.PREFIX_RESUME_DURATION + mediaId

    private companion object {
        /** Positions closer than this to the end count as finished. */
        const val NEAR_END_MS = 8_000L
    }
}