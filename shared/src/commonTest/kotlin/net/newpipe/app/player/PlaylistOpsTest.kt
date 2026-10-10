/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaylistOpsTest {

    private fun item(id: String) = MediaItem(id = id, title = "Track $id", url = "file:///$id")

    private fun queue(vararg ids: String) = PlayQueue(ids.map(::item))

    @Test
    fun queueToPlaylistToQueuePreservesItemOrder() {
        val original = queue("a", "b", "c")

        val restored = original.toSavedPlaylist("Mix").toPlayQueue()

        assertEquals(listOf("a", "b", "c"), restored.items.map { it.id })
    }

    @Test
    fun toSavedPlaylistUsesIdDefaultingToNameAndIgnoresCurrentIndex() {
        val original = queue("a", "b", "c")
        original.jumpTo(1)

        val saved = original.toSavedPlaylist("Mix")

        assertEquals("Mix", saved.id)
        assertEquals("Mix", saved.name)
        assertEquals(listOf("a", "b", "c"), saved.items.map { it.id })
    }

    @Test
    fun toSavedPlaylistHonoursExplicitId() {
        val saved = queue("a").toSavedPlaylist(name = "Mix", id = "mix-1")
        assertEquals("mix-1", saved.id)
        assertEquals("Mix", saved.name)
    }

    @Test
    fun toPlayQueueItemsEqualPlaylistItems() {
        val saved = SavedPlaylist(id = "x", name = "X", items = listOf(item("1"), item("2")))

        val q = saved.toPlayQueue()

        assertEquals(saved.items, q.items)
        assertEquals("1", q.current?.id)
    }

    @Test
    fun toPlayQueueOfEmptyPlaylistYieldsEmptyQueue() {
        val q = SavedPlaylist(id = "x", name = "X", items = emptyList()).toPlayQueue()
        assertTrue(q.isEmpty)
    }

    @Test
    fun skipUnavailableDropsOnlyUnavailableAndKeepsOrder() {
        val items = listOf(item("a"), item("b"), item("c"), item("d"))

        val kept = items.skipUnavailable { it.id != "b" && it.id != "d" }

        assertEquals(listOf("a", "c"), kept.map { it.id })
    }

    @Test
    fun skipUnavailableKeepsEverythingWhenAllAvailable() {
        val items = listOf(item("a"), item("b"))
        assertEquals(items, items.skipUnavailable { true })
    }

    @Test
    fun skipUnavailableOnEmptyListReturnsEmptyWithoutThrowing() {
        assertTrue(emptyList<MediaItem>().skipUnavailable { false }.isEmpty())
    }
}
