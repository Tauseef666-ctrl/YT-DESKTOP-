/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import java.awt.Desktop
import java.io.File

/** Desktop bridge to the OS file browser; never throws, reports success through the return value. */
object SystemFileActions {

    /** Opens [path] with the default application. Returns true when handed off to the OS. */
    fun openFile(path: String): Boolean {
        if (path.isBlank()) return false
        return try {
            val file = File(path)
            if (!file.exists()) return false
            Desktop.getDesktop().open(file)
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Opens the folder for [path]: the directory itself, or the parent of a file. Returns true when
     * handed off to the OS.
     */
    fun openFolder(path: String): Boolean {
        if (path.isBlank()) return false
        val file = File(path)
        val folder = if (file.isDirectory) file else file.parentFile ?: return false
        if (!folder.exists()) return false
        return try {
            Desktop.getDesktop().open(folder)
            true
        } catch (_: Exception) {
            false
        }
    }
}

actual fun openDownloadedFile(path: String): Boolean = SystemFileActions.openFile(path)

actual fun openDownloadedFileFolder(path: String): Boolean = SystemFileActions.openFolder(path)
