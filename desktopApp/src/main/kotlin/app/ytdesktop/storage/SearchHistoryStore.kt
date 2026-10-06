/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.storage

import java.io.File

/**
 * Search history persistence (plan.md 1.4). Most-recent-first, deduped and
 * capped; a plain `user.home/.yt-desktop/search-history.txt` keeps it free of
 * extra dependencies. Android history lands later on its own (DataStore);
 * desktop's lifecycle is owned by Main.
 */
class SearchHistoryStore(private val file: File) {

    fun load(): List<String> =
        runCatching { file.readLines().map(String::trim).filter(String::isNotBlank) }.getOrDefault(emptyList())

    fun append(term: String) {
        if (term.isBlank()) return
        val updated = (listOf(term) + load().filter { it != term }).take(MAX_ENTRIES)
        runCatching { file.parentFile.mkdirs(); file.writeText(updated.joinToString("\n")) }
    }

    fun clear() = runCatching { file.delete() }

    companion object {
        private const val MAX_ENTRIES = 20

        fun inDefaultDir(): SearchHistoryStore {
            val dir = File(System.getProperty("user.home"), ".yt-desktop")
            return SearchHistoryStore(File(dir, "search-history.txt"))
        }
    }
}