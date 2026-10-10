/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Singleton

/**
 * Per-media playback overrides (F2), keyed by [MediaItem.id]. A `null` [quality] means automatic
 * selection and neutral defaults mirror the engine's baseline.
 */
@Serializable
data class VideoSettings(
    val speed: Float = 1.0f,
    val volumePercent: Int = 100,
    val muted: Boolean = false,
    val quality: Quality? = null
)

/** Serializable wrapper so the whole media->settings map is stored under a single key. */
@Serializable
data class VideoSettingsSnapshot(
    val entries: Map<String, VideoSettings>
)

/**
 * Clamps a [VideoSettings] into the supported ranges (speed `0.25f..3.0f`, volume `0..100`).
 * Pure and side-effect free so both the store and the future UI can rely on it.
 */
fun normalize(v: VideoSettings): VideoSettings = v.copy(
    speed = v.speed.coerceIn(0.25f, 3.0f),
    volumePercent = v.volumePercent.coerceIn(0, 100)
)

/**
 * Persists per-media playback overrides to [Settings] so a user's speed/volume/quality choices
 * survive an app restart (F2). Writes are merged so unrelated media keep their entries.
 */
@Singleton
class VideoSettingsStore(
    private val settings: Settings,
    private val json: Json
) {
    fun save(mediaId: String, video: VideoSettings) {
        val entries = loadAll() + (mediaId to video)
        settings.putString(KEY, json.encodeToString(VideoSettingsSnapshot(entries)))
    }

    fun load(mediaId: String): VideoSettings? = loadAll()[mediaId]

    fun loadAll(): Map<String, VideoSettings> = load()?.entries ?: emptyMap()

    fun clear(mediaId: String) {
        val remaining = loadAll() - mediaId
        if (remaining.isEmpty()) {
            settings.remove(KEY)
        } else {
            settings.putString(KEY, json.encodeToString(VideoSettingsSnapshot(remaining)))
        }
    }

    fun clearAll() {
        settings.remove(KEY)
    }

    /** Returns the stored snapshot, or null when none/blank/corrupt (corrupt data is dropped). */
    fun load(): VideoSettingsSnapshot? {
        val raw = settings.getString(KEY, "")
        if (raw.isBlank()) return null
        return try {
            json.decodeFromString<VideoSettingsSnapshot>(raw)
                .takeIf { it.entries.isNotEmpty() }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    companion object {
        const val KEY = "npp_video_settings"
    }
}
