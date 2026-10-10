/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.download

import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadStoreTest {

    private fun store(settings: MapSettings = MapSettings()) =
        DownloadStore(settings, Json)

    private fun task(
        id: String,
        status: DownloadState = DownloadState.QUEUED,
        downloadedBytes: Long = 0L,
        totalBytes: Long = 0L,
        error: String? = null
    ) = DownloadTask(
        request = DownloadRequest(
            id = id,
            url = "http://127.0.0.1/$id",
            destinationPath = "C:/Downloads/$id.bin",
            title = id
        ),
        status = status,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        filePath = if (status == DownloadState.COMPLETED) "C:/Downloads/$id.bin" else null,
        error = error
    )

    @Test
    fun saveAndLoadRoundTrip() {
        val s = store()
        s.save(DownloadSnapshot(listOf(task("a"), task("b", DownloadState.PAUSED, 42))))

        val loaded = s.load()?.tasks
        assertTrue(loaded != null)
        assertEquals(listOf("a", "b"), loaded.map { it.id })
        assertEquals(DownloadState.PAUSED, loaded[1].status)
        assertEquals(42L, loaded[1].downloadedBytes)
    }

    @Test
    fun loadReturnsNullWhenNothingSaved() {
        assertNull(store().load())
    }

    @Test
    fun loadReturnsNullForBlankOrCorruptData() {
        val settings = MapSettings()
        settings.putString(DownloadStore.KEY, "")
        assertNull(DownloadStore(settings, Json).load())

        settings.putString(DownloadStore.KEY, "not json {")
        assertNull(DownloadStore(settings, Json).load())
    }

    @Test
    fun loadReturnsNullForEmptyTaskList() {
        val s = store()
        s.save(DownloadSnapshot(emptyList()))
        assertNull(s.load())
    }

    @Test
    fun clearRemovesSavedSnapshotAndLeavesOtherKeysAlone() {
        val settings = MapSettings()
        settings.putString("other_key", "keep")
        val s = DownloadStore(settings, Json)
        s.save(DownloadSnapshot(listOf(task("a"))))
        s.clear()

        assertNull(s.load())
        assertFalse(settings.hasKey(DownloadStore.KEY))
        assertEquals("keep", settings.getString("other_key", ""))
    }

    @Test
    fun restoreTasksDropsActiveStatusAndSpeed() {
        val snapshot = DownloadSnapshot(
            listOf(
                task("dl", DownloadState.DOWNLOADING, 10, 100),
                task("p", DownloadState.PAUSED, 20, 100),
                task("c", DownloadState.COMPLETED, 100, 100),
                task("f", DownloadState.FAILED, 5, 100, "boom")
            )
        )

        val restored = DownloadStore.restoreTasks(snapshot)
        val byId = restored.associateBy { it.id }

        assertEquals(DownloadState.QUEUED, byId["dl"]?.status)
        assertEquals(DownloadState.PAUSED, byId["p"]?.status)
        assertEquals(DownloadState.COMPLETED, byId["c"]?.status)
        assertEquals(DownloadState.FAILED, byId["f"]?.status)
        assertTrue(restored.all { it.bytesPerSecond == 0L }, "speed estimate must be reset on restore")
    }
}