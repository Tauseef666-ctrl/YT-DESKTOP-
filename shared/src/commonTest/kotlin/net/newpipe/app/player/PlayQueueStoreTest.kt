/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import com.russhwolf.settings.MapSettings
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayQueueStoreTest {

    private fun store(settings: MapSettings = MapSettings()) =
        PlayQueueStore(settings, Json)

    private fun snapshot() = PlayQueueSnapshot(
        items = listOf(
            MediaItem(id = "1", title = "One", url = "file:///1", durationMs = 60_000L),
            MediaItem(id = "2", title = "Two", url = "file:///2")
        ),
        currentIndex = 1,
        repeatMode = RepeatMode.ALL,
        isShuffled = true
    )

    @Test
    fun saveAndLoadRoundTrip() {
        val s = store()
        s.save(snapshot())

        val loaded = s.load()
        assertEquals(listOf("1", "2"), loaded?.items?.map { it.id })
        assertEquals(1, loaded?.currentIndex)
        assertEquals(RepeatMode.ALL, loaded?.repeatMode)
        assertEquals(true, loaded?.isShuffled)
    }

    @Test
    fun loadReturnsNullWhenNothingSaved() {
        assertNull(store().load())
    }

    @Test
    fun loadReturnsNullForBlankOrCorruptData() {
        val settings = MapSettings()
        settings.putString("npp_persisted_queue", "")
        assertNull(PlayQueueStore(settings, Json).load())

        settings.putString("npp_persisted_queue", "not json {")
        assertNull(PlayQueueStore(settings, Json).load())
    }

    @Test
    fun loadReturnsNullForEmptyItemList() {
        val s = store()
        s.save(PlayQueueSnapshot(emptyList(), -1, RepeatMode.OFF, false))
        assertNull(s.load())
    }

    @Test
    fun clearRemovesSavedQueue() {
        val s = store()
        s.save(snapshot())
        s.clear()
        assertNull(s.load())
    }

    @Test
    fun mediaItemFieldsSurviveSerialization() {
        val original = MediaItem(
            id = "live", title = "Live", url = "file:///live", durationMs = 0L,
            isLive = true, audioOnly = true, thumbnailUrl = "https://x/t.jpg"
        )
        val s = store()
        s.save(PlayQueueSnapshot(listOf(original), 0, RepeatMode.OFF, false))

        val loaded = s.load()?.items?.single()
        assertEquals(original, loaded)
    }

    @Test
    fun unrelatedSettingsAreUntouched() {
        val settings = MapSettings()
        settings.putString("other_key", "keep")
        val s = PlayQueueStore(settings, Json)
        s.save(snapshot())
        s.clear()
        assertTrue(s.load() == null)
        assertEquals("keep", settings.getString("other_key", ""))
    }
}