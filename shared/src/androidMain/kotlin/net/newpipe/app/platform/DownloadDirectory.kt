/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

/**
 * Android downloads are managed through Storage Access Framework / MediaStore; a plain absolute
 * path is not usable as a default, so the desktop-style download screen reports an empty folder.
 */
actual fun defaultDownloadDirectory(): String = ""