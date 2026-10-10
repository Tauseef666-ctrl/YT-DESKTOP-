/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

/**
 * Absolute path of the platform's default download directory (no trailing separator).
 * Returns an empty string on platforms without a usable default.
 */
expect fun defaultDownloadDirectory(): String