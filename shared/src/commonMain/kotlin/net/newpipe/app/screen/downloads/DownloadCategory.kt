/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.downloads

import net.newpipe.app.download.DownloadState
import net.newpipe.app.download.DownloadTask

/** Mission-list filter; [ALL] is the unfiltered view, the rest map one-to-one onto a status. */
enum class DownloadCategory {
    ALL,
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED;

    companion object {
        /** Fixed tab-bar order. */
        val tabsInOrder: List<DownloadCategory> = entries.toList()

        /** The single-status category a mission belongs to; never [ALL]. */
        fun of(status: DownloadState): DownloadCategory = when (status) {
            DownloadState.QUEUED -> QUEUED
            DownloadState.DOWNLOADING -> DOWNLOADING
            DownloadState.PAUSED -> PAUSED
            DownloadState.COMPLETED -> COMPLETED
            DownloadState.FAILED -> FAILED
        }
    }
}

/** Missions in this category, preserving input order; [DownloadCategory.ALL] passes through. */
fun DownloadCategory.select(tasks: List<DownloadTask>): List<DownloadTask> =
    if (this == DownloadCategory.ALL) {
        tasks
    } else {
        tasks.filter { DownloadCategory.of(it.status) == this }
    }

/** Mission count per category; [DownloadCategory.ALL] counts every mission. */
fun tabCounts(tasks: List<DownloadTask>): Map<DownloadCategory, Int> =
    DownloadCategory.entries.associateWith { category ->
        if (category == DownloadCategory.ALL) {
            tasks.size
        } else {
            tasks.count { DownloadCategory.of(it.status) == category }
        }
    }
