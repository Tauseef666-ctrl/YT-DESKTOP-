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

/** A named, user-saved list of [MediaItem]s that can be restored into a [PlayQueue]. */
@Serializable
data class SavedPlaylist(
    val id: String,
    val name: String,
    val items: List<MediaItem>
)

/** Whole-store snapshot persisted as a single serialized value. */
@Serializable
data class PlaylistSnapshot(
    val playlists: List<SavedPlaylist>
)

/**
 * Persists named playlists to [Settings] so they survive an app restart. All playlists are stored
 * as one JSON snapshot under a single key; a missing/blank/corrupt/empty store reads back as null
 * and corrupt data is dropped.
 */
@Singleton
class PlaylistStore(
    private val settings: Settings,
    private val json: Json
) {
    private val key = "npp_saved_playlists"

    fun saveAll(snapshot: PlaylistSnapshot) {
        settings.putString(key, json.encodeToString(snapshot))
    }

    /** Returns the saved snapshot, or null when none/blank/corrupt/empty (corrupt data is dropped). */
    fun loadAll(): PlaylistSnapshot? {
        val raw = settings.getString(key, "")
        if (raw.isBlank()) return null
        return try {
            json.decodeFromString<PlaylistSnapshot>(raw)
                .takeIf { it.playlists.isNotEmpty() }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Insert [playlist], replacing an existing entry with the same id while preserving order. */
    fun save(playlist: SavedPlaylist) {
        val existing = loadAll()?.playlists ?: emptyList()
        val index = existing.indexOfFirst { it.id == playlist.id }
        val updated = if (index >= 0) {
            existing.toMutableList().apply { this[index] = playlist }
        } else {
            existing + playlist
        }
        saveAll(PlaylistSnapshot(updated))
    }

    /** Remove the playlist with [id]; other playlists are kept. */
    fun delete(id: String) {
        val existing = loadAll()?.playlists ?: return
        saveAll(PlaylistSnapshot(existing.filterNot { it.id == id }))
    }

    fun load(id: String): SavedPlaylist? = loadAll()?.playlists?.firstOrNull { it.id == id }

    fun clear() {
        settings.remove(key)
    }
}
