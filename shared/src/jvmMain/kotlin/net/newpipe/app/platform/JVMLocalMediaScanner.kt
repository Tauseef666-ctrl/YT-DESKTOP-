/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.newpipe.app.player.LocalMediaFile
import net.newpipe.app.player.LocalMediaFormats
import net.newpipe.app.player.LocalMediaScanner
import net.newpipe.app.player.ScanDiff
import net.newpipe.app.player.diffByPath
import org.koin.core.annotation.Singleton
import java.io.File

/**
 * Desktop folder scanner. Recursively walks a folder and reports every file whose extension is a
 * known playable container. Only metadata that is actually available is reported: duration is left
 * `null` until the engine learns it during playback — it is never guessed.
 */
@Singleton(binds = [LocalMediaScanner::class])
class JVMLocalMediaScanner : LocalMediaScanner {

    override val isAvailable: Boolean = true

    override suspend fun scanFolder(folderPath: String): List<LocalMediaFile> =
        withContext(Dispatchers.IO) {
            val root = File(folderPath)
            require(root.exists()) { "Folder does not exist: $folderPath" }
            require(root.isDirectory) { "Not a folder: $folderPath" }
            require(root.canRead()) { "Folder is not readable: $folderPath" }

            root.walkTopDown()
                .onEnter { it.canRead() }
                .filter { it.isFile }
                .mapNotNull { toMediaFile(it) }
                .sortedBy { it.name.lowercase() }
                .toList()
        }

    /**
     * Compare the current contents of [dir] against [previous]. Files whose modification time or
     * size changed, plus brand new files, are reported in the resulting [ScanDiff]; paths no longer
     * present are reported as removed.
     */
    fun incrementalScan(dir: File, previous: List<LocalMediaFile>): ScanDiff {
        val root = dir.absoluteFile
        require(root.exists()) { "Folder does not exist: ${root.absolutePath}" }
        require(root.isDirectory) { "Not a folder: ${root.absolutePath}" }
        require(root.canRead()) { "Folder is not readable: ${root.absolutePath}" }

        return diffByPath(previous, collectMedia(root))
    }

    private fun collectMedia(root: File): List<LocalMediaFile> =
        root.walkTopDown()
            .onEnter { it.canRead() }
            .filter { it.isFile }
            .mapNotNull { toMediaFile(it) }
            .sortedBy { it.name.lowercase() }
            .toList()

    private fun toMediaFile(file: File): LocalMediaFile? {
        val extension = file.extension.lowercase()
        if (!LocalMediaFormats.isSupported(extension)) return null
        return LocalMediaFile(
            id = file.absolutePath,
            path = file.absolutePath,
            name = file.name,
            extension = extension,
            mimeType = LocalMediaFormats.mimeType(extension),
            url = file.toURI().toString(),
            sizeBytes = file.length(),
            modifiedAt = file.lastModified(),
            durationMs = null,
            audioOnly = LocalMediaFormats.isAudio(extension)
        )
    }
}
