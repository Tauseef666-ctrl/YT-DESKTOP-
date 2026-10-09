/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.storage

import java.io.File

/**
 * Remembers the folder the user chose to browse for already-downloaded videos,
 * so the Library is one click away on the next launch.
 */
class LibraryFolderStore(private val file: File) {

    fun load(): String? =
        runCatching { file.readLines().firstOrNull { it.isNotBlank() } }.getOrNull()

    fun save(path: String) {
        runCatching {
            file.parentFile.mkdirs()
            file.writeText(path)
        }
    }

    companion object {
        fun inDefaultDir(): LibraryFolderStore {
            val dir = File(System.getProperty("user.home"), ".yt-desktop")
            return LibraryFolderStore(File(dir, "library-folder.txt"))
        }
    }
}