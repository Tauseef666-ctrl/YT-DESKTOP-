/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.download

import app.ytdesktop.core.downloader.HttpDownloader
import java.nio.file.Path

/**
 * One file to fetch. [destination] is written in place and is also the resume
 * cursor: on restart, already-complete whole blocks are skipped (the trailing
 * torn block of a previous run is truncated back to the block boundary).
 */
data class DownloadRequest(
    val url: String,
    val destination: Path,
    /** Known size in bytes; if `null` the engine probes with HEAD first. */
    val expectedSize: Long? = null,
    val userAgent: String = HttpDownloader.DEFAULT_USER_AGENT,
    /** Overrides applied to every request (e.g. an extra auth header). */
    val headerOverrides: Map<String, String> = emptyMap(),
    /**
     * Recovery hook (plan 2.2): called when a range request fails with a hard
     * HTTP error, typically 403. Return a replacement URL (e.g. a freshly
     * re-extracted stream URL) and the engine retries the failed block against
     * it; return `null` to give up and surface [DownloadException].
     */
    val onHttpError: suspend (code: Int, url: String) -> String? = { _, _ -> null },
)

/** Snapshot delivered after every completed block (thread-safe, monotonic). */
data class DownloadProgress(
    val bytesDone: Long,
    val totalBytes: Long?,
    val activeBlocks: Int,
)

data class DownloadedFile(
    val file: Path,
    val bytes: Long,
)