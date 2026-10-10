/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.library

import kotlin.test.Test
import kotlin.test.assertEquals
import net.newpipe.app.player.LocalMediaFile

class LibraryFilterTest {

    private fun file(
        name: String,
        path: String = "/media/$name",
        audioOnly: Boolean = false,
        sizeBytes: Long = 0L,
        modifiedAt: Long = 0L
    ): LocalMediaFile = LocalMediaFile(
        id = path,
        path = path,
        name = name,
        extension = name.substringAfterLast('.', ""),
        mimeType = if (audioOnly) "audio/x" else "video/x",
        url = "file://$path",
        sizeBytes = sizeBytes,
        modifiedAt = modifiedAt,
        audioOnly = audioOnly
    )

    @Test
    fun queryMatchesNameCaseInsensitive() {
        val items = listOf(file("Mission Impossible.mkv"), file("Tutorial.mp4"))
        val result = applyFilters(items, LibraryFilters(query = "MISSION"))
        assertEquals(listOf("Mission Impossible.mkv"), result.map { it.name })
    }

    @Test
    fun queryMatchesPath() {
        val target = file("clip.mp4", path = "/home/user/Documents/secret/clip.mp4")
        val other = file("other.mp4", path = "/home/user/Videos/other.mp4")
        val result = applyFilters(listOf(other, target), LibraryFilters(query = "documents"))
        assertEquals(listOf(target), result)
    }

    @Test
    fun queryTrims() {
        val items = listOf(file("Mission Impossible.mkv"), file("Tutorial.mp4"))
        val result = applyFilters(items, LibraryFilters(query = "  mission  "))
        assertEquals(listOf("Mission Impossible.mkv"), result.map { it.name })
    }

    @Test
    fun allKeepsEverything() {
        val items = listOf(file("a.mp4"), file("b.flac", audioOnly = true), file("c.mkv"))
        val result = applyFilters(items, LibraryFilters(filter = MediaFilter.ALL))
        assertEquals(items, result)
    }

    @Test
    fun videosExcludesAudioOnly() {
        val video = file("a.mp4")
        val audio = file("b.flac", audioOnly = true)
        val result = applyFilters(listOf(video, audio), LibraryFilters(filter = MediaFilter.VIDEOS))
        assertEquals(listOf(video), result)
    }

    @Test
    fun audioOnlyShowsOnlyAudio() {
        val video = file("a.mp4")
        val audio = file("b.flac", audioOnly = true)
        val result = applyFilters(listOf(video, audio), LibraryFilters(filter = MediaFilter.AUDIO))
        assertEquals(listOf(audio), result)
    }

    @Test
    fun sortByNameAscendingStable() {
        val firstB = file("b.mp4")
        val firstA = file("a.mp4", path = "/media/a.mp4")
        val secondA = file("a.mp4", path = "/other/a.mp4")
        val result = applyFilters(listOf(firstB, firstA, secondA), LibraryFilters(sort = SortOrder.NAME))
        assertEquals(listOf("/media/a.mp4", "/other/a.mp4", "/media/b.mp4"), result.map { it.path })
    }

    @Test
    fun sortBySizeDescending() {
        val small = file("a.mp4", sizeBytes = 10L)
        val large = file("b.mp4", sizeBytes = 100L)
        val medium = file("c.mp4", sizeBytes = 50L)
        val result = applyFilters(
            listOf(small, large, medium),
            LibraryFilters(sort = SortOrder.SIZE)
        )
        assertEquals(listOf(large, medium, small), result)
    }

    @Test
    fun sortByNewestDescending() {
        val old = file("a.mp4", modifiedAt = 100L)
        val newest = file("b.mp4", modifiedAt = 300L)
        val middle = file("c.mp4", modifiedAt = 200L)
        val result = applyFilters(
            listOf(old, newest, middle),
            LibraryFilters(sort = SortOrder.NEWEST)
        )
        assertEquals(listOf(newest, middle, old), result)
    }

    @Test
    fun combinedQueryFilterAndSort() {
        val items = listOf(
            file("mission.mp4", sizeBytes = 30L),
            file("mission.flac", audioOnly = true, sizeBytes = 40L),
            file("mission-2.mp4", sizeBytes = 10L),
            file("tutorial.mp4", sizeBytes = 50L)
        )
        val result = applyFilters(
            items,
            LibraryFilters(
                query = "mission",
                filter = MediaFilter.VIDEOS,
                sort = SortOrder.SIZE
            )
        )
        assertEquals(listOf("mission.mp4", "mission-2.mp4"), result.map { it.name })
    }

    @Test
    fun emptyItemsYieldsEmpty() {
        assertEquals(emptyList(), applyFilters(emptyList(), LibraryFilters()))
    }

    @Test
    fun defaultFiltersReturnOriginalOrder() {
        val items = listOf(file("a.mp4"), file("b.mp4"), file("c.mp4"))
        assertEquals(items, applyFilters(items, LibraryFilters()))
    }
}
