/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

/**
 * Opens [path] with the platform's default handler. Returns false when the path is unusable,
 * the file does not exist or the platform cannot open files.
 */
expect fun openDownloadedFile(path: String): Boolean

/**
 * Reveals the folder containing [path] (the folder itself for a directory, its parent otherwise).
 * Returns false when the path is unusable, missing or the platform cannot open folders.
 */
expect fun openDownloadedFileFolder(path: String): Boolean
