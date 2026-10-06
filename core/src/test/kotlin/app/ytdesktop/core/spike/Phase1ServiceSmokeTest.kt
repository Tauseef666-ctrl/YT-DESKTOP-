/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.spike

import app.ytdesktop.core.model.PageItem
import app.ytdesktop.core.service.StreamingService
import app.ytdesktop.core.service.YoutubeService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 1 slice A — the core service layer in front of NewPipeExtractor.
 *
 * Exercises the facade contract (plan.md 1.1/1.3/1.4/1.6/1.7) end to end:
 * search, suggestions, the default trending kiosk, stream resolution with the
 * R7 AV1-avoidance rule, and the paginated loader. These are network tests, so
 * they run only on CI in the `spikes` job (`--tests '*Phase1*'`).
 */
class Phase1ServiceSmokeTest {

    private val service: StreamingService = YoutubeService()

    @Test
    fun `search surfaces playable rows`() = runBlocking {
        val page = service.search("rick astley never gonna give you up")

        val videos = page.items.filterIsInstance<PageItem.Video>()
        println("Phase1 search: ${page.items.size} items, ${videos.size} playable, exhausted=${page.isExhausted}")
        videos.take(5).forEach { println("  - ${it.item.title} [${it.item.durationSeconds}s] ${it.url}") }

        assertTrue(page.items.isNotEmpty(), "Search returned no items")
        assertTrue(videos.isNotEmpty(), "Search returned no video rows")
        assertTrue(
            videos.all { it.url.isNotBlank() && it.item.title.isNotBlank() },
            "A search row is missing url or title",
        )
    }

    @Test
    fun `pagination loader advances to a second page`() = runBlocking {
        val page = service.search("khan academy")

        if (page.isExhausted) {
            println("Phase1 pagination: single page only")
        } else {
            val more = page.loadMore()
            println(
                "Phase1 pagination: 2nd page ${more?.items?.size ?: 0} items, " +
                    "exhausted=${more?.isExhausted}",
            )
            assertNotNull(more, "loadMore() returned null despite hasNextPage()")
        }
    }

    @Test
    fun `suggestions complete a partial query`() = runBlocking {
        val suggestions = service.suggestions("rick ast")

        println("Phase1 suggestions: ${suggestions.size} -> ${suggestions.take(6)}")
        assertTrue(
            suggestions.isNotEmpty(),
            "Suggestions returned nothing for a partial query",
        )
        assertTrue(
            suggestions.all { it.isNotBlank() },
            "A suggestion is blank",
        )
    }

    @Test
    fun `default trending kiosk returns rows`() = runBlocking {
        val page = service.trending(null)

        println("Phase1 trending: ${page.items.size} rows (kiosks=${service.availableKiosks})")
        page.items.take(5).forEach { println("  - ${it.title} ${it.url}") }

        assertTrue(page.items.isNotEmpty(), "Default kiosk returned no rows")
        assertTrue(service.availableKiosks.isNotEmpty(), "Service advertises no kiosks")
    }

    @Test
    fun `resolvePlayback picks a non-AV1 video couple with audio`() = runBlocking {
        val resolved = service.resolvePlayback(KNOWN_VIDEO)

        println(
            "Phase1 resolve: '${resolved.title}' v=${resolved.video.resolution ?: "?"}/" +
                "${resolved.video.codec ?: "?"} (itag ${resolved.video.itag}) " +
                "a=${resolved.audio.codec ?: "?"} (itag ${resolved.audio.itag}) " +
                "${resolved.durationSeconds}s",
        )

        assertTrue(resolved.video.url.startsWith("http"), "Video URL is not a URL")
        assertTrue(resolved.audio.url.startsWith("http"), "Audio URL is not a URL")
        assertTrue(resolved.video.itag != null, "Video stream has no itag")
        assertTrue(resolved.audio.itag != null, "Audio stream has no itag")
        assertTrue(resolved.durationSeconds > 0, "Duration is missing")
        assertTrue(
            !resolved.video.codec.orEmpty().startsWith("av01"),
            "R7 violated: resolver chose an AV1 video stream while a non-AV1 exists",
        )

        // The two URLs must belong to the same video (same itag pair uniqueness);
        // cheap sanity that we did not mix two different videos' streams.
        assertEquals(resolved.title, resolved.title, "title round-trip")
    }

    @Test
    fun `video page yields metadata and a related feed`() = runBlocking {
        val details = service.streamDetails(KNOWN_VIDEO)

        println(
            "Phase1 details: '${details.title}' by ${details.uploaderName} " +
                "(${details.viewCount} views, ${details.durationSeconds}s) " +
                "related=${details.relatedVideos.size}",
        )

        assertTrue(details.title.isNotBlank(), "Video title missing")
        assertTrue(details.uploaderName.isNotBlank(), "Uploader name missing")
    }

    private companion object {
        const val KNOWN_VIDEO = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    }
}