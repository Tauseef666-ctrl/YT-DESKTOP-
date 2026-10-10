/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import net.newpipe.app.player.LocalMediaFile
import net.newpipe.app.player.LocalMediaScanner
import net.newpipe.app.player.PlaybackController
import net.newpipe.app.preferences.LibraryPreferences
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the local-media library: the set of indexed folders (persisted), folder scanning via the
 * platform [LocalMediaScanner], and hand-off of a scanned list to the shared [PlaybackController].
 */
@KoinViewModel
class LibraryViewModel(
    private val scanner: LocalMediaScanner,
    private val settings: Settings,
    private val playback: PlaybackController
) : ViewModel() {

    val isScannerAvailable: Boolean = scanner.isAvailable

    val folders: StateFlow<List<String>>
        field = MutableStateFlow(loadFolders())

    val items: StateFlow<List<LocalMediaFile>>
        field = MutableStateFlow(emptyList())

    val isScanning: StateFlow<Boolean>
        field = MutableStateFlow(false)

    val scanError: StateFlow<String?>
        field = MutableStateFlow(null)

    private fun loadFolders(): List<String> =
        settings.getString(
            LibraryPreferences.KEY_INDEXED_FOLDERS,
            LibraryPreferences.DEFAULT_INDEXED_FOLDERS
        )
            .split(LibraryPreferences.FOLDER_SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun saveFolders(list: List<String>) {
        settings.putString(
            LibraryPreferences.KEY_INDEXED_FOLDERS,
            list.joinToString(LibraryPreferences.FOLDER_SEPARATOR)
        )
    }

    fun addFolder(path: String) {
        val trimmed = path.trim()
        if (trimmed.isEmpty() || trimmed in folders.value) return
        folders.value = folders.value + trimmed
        saveFolders(folders.value)
    }

    fun removeFolder(path: String) {
        if (path !in folders.value) return
        folders.value = folders.value - path
        saveFolders(folders.value)
    }

    /** Re-scan every indexed folder. Unreadable folders are reported, not silently dropped. */
    fun rescan() {
        val roots = folders.value
        viewModelScope.launch {
            isScanning.value = true
            scanError.value = null
            val collected = mutableListOf<LocalMediaFile>()
            val errors = mutableListOf<String>()
            for (root in roots) {
                try {
                    collected += scanner.scanFolder(root)
                } catch (t: Throwable) {
                    errors += "$root: ${t.message ?: "unreadable"}"
                }
            }
            items.value = collected
            scanError.value = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
            isScanning.value = false
        }
    }

    /** Queue the whole library (starting at [item]) on the shared controller and play it. */
    fun play(item: LocalMediaFile) {
        val current = items.value
        val index = current.indexOf(item)
        if (index < 0) return
        viewModelScope.launch {
            playback.setQueue(current.map { it.toMediaItem() }, index)
        }
    }
}
