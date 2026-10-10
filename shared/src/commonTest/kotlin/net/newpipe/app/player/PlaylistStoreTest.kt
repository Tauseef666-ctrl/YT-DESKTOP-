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

class PlaylistStoreTest {

    private fun store(settings: MapSettings = MapSettings()) =
        PlaylistStore(settings, Json)

    private fun item(id: String) = MediaItem(id = id, title = "Track $id", url = "file:///$id")

    private fun playlist(id: String, vararg items: String) =
        SavedPlaylist(id = id, name = "List $id", items = items.map(::item))

    @Test
    fun saveAndLoadAllRoundTripKeepsItemsAndOrder() {
        val s = store()
        s.saveAll(
            PlaylistSnapshot(
                listOf(
                    playlist("a", "1", "2"),
                    playlist("b", "3")
                )
            )
        )

        val loaded = s.loadAll()
        assertEquals(listOf("a", "b"), loaded?.playlists?.map { it.id })
        assertEquals("List a", loaded?.playlists?.first()?.name)
        assertEquals(listOf("1", "2"), loaded?.playlists?.first()?.items?.map { it.id })
    }

    @Test
    fun saveReplacesSameIdAndKeepsOthers() {
        val s = store()
        s.save(playlist("a", "1"))
        s.save(playlist("b", "2"))
        s.save(playlist("a", "9", "8"))

        val playlists = s.loadAll()?.playlists
        assertEquals(listOf("a", "b"), playlists?.map { it.id })
        assertEquals(listOf("9", "8"), playlists?.first()?.items?.map { it.id })
    }

    @Test
    fun deleteRemovesOnlyTarget() {
        val s = store()
        s.save(playlist("a", "1"))
        s.save(playlist("b", "2"))
        s.save(playlist("c", "3"))

        s.delete("b")

        assertEquals(listOf("a", "c"), s.loadAll()?.playlists?.map { it.id })
    }

    @Test
    fun loadReturnsRequestedPlaylist() {
        val s = store()
        s.save(playlist("a", "1"))
        s.save(playlist("b", "2", "3"))

        assertEquals(listOf("2", "3"), s.load("b")?.items?.map { it.id })
        assertNull(s.load("missing"))
    }

    @Test
    fun loadAllReturnsNullForBlankOrCorruptData() {
        val settings = MapSettings()
        settings.putString("npp_saved_playlists", "")
        assertNull(PlaylistStore(settings, Json).loadAll())

        settings.putString("npp_saved_playlists", "not json {")
        assertNull(PlaylistStore(settings, Json).loadAll())
    }

    @Test
    fun loadAllReturnsNullForEmptySnapshot() {
        val s = store()
        s.saveAll(PlaylistSnapshot(emptyList()))
        assertNull(s.loadAll())
    }

    @Test
    fun clearRemovesKeyAndLeavesUnrelatedKeysUntouched() {
        val settings = MapSettings()
        settings.putString("other_key", "keep")
        val s = PlaylistStore(settings, Json)
        s.save(playlist("a", "1"))

        s.clear()

        assertNull(s.loadAll())
        assertEquals("keep", settings.getString("other_key", ""))
    }

    @Test
    fun mediaItemFieldsSurviveSerialization() {
        val original = MediaItem(
            id = "live", title = "Live", url = "file:///live", durationMs = 0L,
            isLive = true, audioOnly = true, thumbnailUrl = "https://x/t.jpg"
        )
        val s = store()
        s.save(SavedPlaylist(id = "x", name = "X", items = listOf(original)))

        assertEquals(original, s.load("x")?.items?.single())
    }

    @Test
    fun savedPlaylistsAreIndependentInstances() {
        val s = store()
        s.save(playlist("a", "1"))
        s.save(playlist("b", "2"))
        assertTrue(s.load("a") !== s.load("b"))
    }
}
