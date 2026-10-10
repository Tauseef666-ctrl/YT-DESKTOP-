/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.preferences

/**
 * Additive preference keys for the local-media library. These are new keys only; no existing
 * NewPipe preference key is renamed or removed.
 */
object LibraryPreferences {
    const val KEY_INDEXED_FOLDERS = "library_indexed_folders"

    const val DEFAULT_INDEXED_FOLDERS = ""
    const val FOLDER_SEPARATOR = "\n"
}
