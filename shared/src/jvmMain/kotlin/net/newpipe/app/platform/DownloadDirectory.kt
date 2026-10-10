/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import java.io.File

/**
 * On the desktop the default folder is the user's `Downloads` directory. The folder is not
 * auto-created here; creating it is the engine's job when a transfer starts.
 */
actual fun defaultDownloadDirectory(): String =
    File(System.getProperty("user.home"), "Downloads").absolutePath