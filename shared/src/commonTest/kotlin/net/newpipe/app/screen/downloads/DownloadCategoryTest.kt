/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.downloads

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.newpipe.app.download.DownloadRequest
import net.newpipe.app.download.DownloadState
import net.newpipe.app.download.DownloadTask

class DownloadCategoryTest {

    private fun task(id: String, status: DownloadState) = DownloadTask(
        request = DownloadRequest(
            id = id,
            url = "https://example.org/$id",
            destinationPath = "/tmp/$id"
        ),
        status = status
    )

    @Test
    fun ofMapsEachStatusToItsOwnTab() {
        assertEquals(DownloadCategory.QUEUED, DownloadCategory.of(DownloadState.QUEUED))
        assertEquals(DownloadCategory.DOWNLOADING, DownloadCategory.of(DownloadState.DOWNLOADING))
        assertEquals(DownloadCategory.PAUSED, DownloadCategory.of(DownloadState.PAUSED))
        assertEquals(DownloadCategory.COMPLETED, DownloadCategory.of(DownloadState.COMPLETED))
        assertEquals(DownloadCategory.FAILED, DownloadCategory.of(DownloadState.FAILED))
    }

    @Test
    fun ofNeverReturnsAll() {
        DownloadState.entries.forEach { status ->
            assertTrue(DownloadCategory.of(status) != DownloadCategory.ALL)
        }
    }

    @Test
    fun selectAllReturnsEverythingInOrder() {
        val tasks = listOf(
            task("a", DownloadState.QUEUED),
            task("b", DownloadState.COMPLETED),
            task("c", DownloadState.DOWNLOADING)
        )
        assertEquals(tasks, DownloadCategory.ALL.select(tasks))
    }

    @Test
    fun selectFiltersToMatchingStatusAndKeepsOrder() {
        val first = task("a", DownloadState.COMPLETED)
        val second = task("b", DownloadState.DOWNLOADING)
        val third = task("c", DownloadState.COMPLETED)
        assertEquals(
            listOf(first, third),
            DownloadCategory.COMPLETED.select(listOf(first, second, third))
        )
    }

    @Test
    fun selectOnEmptyIsEmpty() {
        assertEquals(emptyList(), DownloadCategory.COMPLETED.select(emptyList()))
    }

    @Test
    fun tabCountsTotalsPerCategory() {
        val tasks = listOf(
            task("a", DownloadState.QUEUED),
            task("b", DownloadState.DOWNLOADING),
            task("c", DownloadState.PAUSED),
            task("d", DownloadState.COMPLETED),
            task("e", DownloadState.COMPLETED),
            task("f", DownloadState.FAILED)
        )
        val counts = tabCounts(tasks)
        assertEquals(6, counts[DownloadCategory.ALL])
        assertEquals(1, counts[DownloadCategory.QUEUED])
        assertEquals(1, counts[DownloadCategory.DOWNLOADING])
        assertEquals(1, counts[DownloadCategory.PAUSED])
        assertEquals(2, counts[DownloadCategory.COMPLETED])
        assertEquals(1, counts[DownloadCategory.FAILED])
    }

    @Test
    fun tabsInOrderIsStableAllFirst() {
        assertEquals(DownloadCategory.ALL, DownloadCategory.tabsInOrder.first())
        assertEquals(DownloadCategory.entries.toList(), DownloadCategory.tabsInOrder)
    }
}