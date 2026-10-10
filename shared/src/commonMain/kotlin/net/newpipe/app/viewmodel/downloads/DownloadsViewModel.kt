/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.downloads

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow
import net.newpipe.app.download.DownloadEngine
import net.newpipe.app.download.DownloadRequest
import net.newpipe.app.download.DownloadTask
import net.newpipe.app.platform.defaultDownloadDirectory
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the downloads screen against the shared [DownloadEngine]. The engine owns all missions and
 * performs real transfers; this ViewModel only forwards user actions and derives display data.
 */
@KoinViewModel
class DownloadsViewModel(
    private val engine: DownloadEngine
) : ViewModel() {

    val isEngineAvailable: Boolean = engine.isAvailable
    val tasks: StateFlow<List<DownloadTask>> = engine.tasks

    /** Default destination folder for new downloads (may be empty on some platforms). */
    val defaultDirectory: String = defaultDownloadDirectory()

    /** Builder for a destination path: [directory]/[name], kept under the given folder. */
    fun destinationIn(directory: String, fileName: String): String {
        val clean = fileName.substringAfterLast('/').substringAfterLast('\\').trim()
        if (clean.isEmpty()) return ""
        val separator = if (directory.contains('\\')) "\\" else "/"
        return directory.trimEnd('\\', '/') + separator + clean
    }

    /** Name derived from a URL (empty when none can be derived). */
    fun fileNameFor(url: String): String {
        val name = url.substringBefore('?').substringAfterLast('/').substringAfterLast('\\').trim()
        return if (name.isEmpty()) name else name
    }

    /** Register and start a download. [destinationPath] must be a real absolute path. */
    fun download(url: String, title: String, destinationPath: String) {
        if (url.isBlank() || destinationPath.isBlank()) return
        engine.enqueue(
            DownloadRequest(
                id = "dl-" + url.hashCode().toUInt().toString(16) + "-" + destinationPath.hashCode().toUInt().toString(16),
                url = url.trim(),
                destinationPath = destinationPath,
                title = title.ifBlank { fileNameFor(url) }
            )
        ).let { engine.start(it.id) }
    }

    fun start(id: String) = engine.start(id)
    fun pause(id: String) = engine.pause(id)
    fun cancel(id: String) = engine.cancel(id)
    fun remove(id: String) = engine.remove(id)
    fun clearCompleted() = engine.clearCompleted()
}