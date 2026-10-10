/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.library

import net.newpipe.app.player.LocalMediaFile

/** Media-type filter applied to the local library. */
enum class MediaFilter { ALL, VIDEOS, AUDIO }

/** Ordering applied to the filtered library list. */
enum class SortOrder { NAME, SIZE, NEWEST }

/** Presentation mode for the filtered library list. */
enum class ViewMode { LIST, GRID }

/**
 * All derived-state inputs for the library screen. Pure and immutable so the composable can hold it
 * in a single `mutableStateOf` and re-derive the visible list on every change.
 */
data class LibraryFilters(
    val query: String = "",
    val filter: MediaFilter = MediaFilter.ALL,
    val sort: SortOrder = SortOrder.NAME,
    val view: ViewMode = ViewMode.LIST
)

/**
 * Pure, deterministic filtering and sorting of [items] according to [filters]. Never mutates the
 * input; returns a fresh list and preserves stable input order for ties.
 *
 * Query matching is trimmed and case-insensitive, and matches either [LocalMediaFile.name] or
 * [LocalMediaFile.path].
 */
fun applyFilters(items: List<LocalMediaFile>, filters: LibraryFilters): List<LocalMediaFile> {
    val query = filters.query.trim().lowercase()

    val byType = when (filters.filter) {
        MediaFilter.ALL -> items
        MediaFilter.VIDEOS -> items.filter { !it.audioOnly }
        MediaFilter.AUDIO -> items.filter { it.audioOnly }
    }

    val byQuery = if (query.isEmpty()) {
        byType
    } else {
        byType.filter { item ->
            item.name.lowercase().contains(query) || item.path.lowercase().contains(query)
        }
    }

    return when (filters.sort) {
        SortOrder.NAME ->
            byQuery.sortedWith(compareBy { it.name.lowercase() })

        SortOrder.SIZE ->
            byQuery.sortedWith(
                compareByDescending<LocalMediaFile> { it.sizeBytes }
                    .thenBy { it.name.lowercase() }
            )

        SortOrder.NEWEST ->
            byQuery.sortedWith(
                compareByDescending<LocalMediaFile> { it.modifiedAt }
                    .thenBy { it.name.lowercase() }
            )
    }
}
