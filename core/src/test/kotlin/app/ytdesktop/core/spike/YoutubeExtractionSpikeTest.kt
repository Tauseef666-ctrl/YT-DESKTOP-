/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.spike

import app.ytdesktop.core.downloader.HttpDownloader
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Spike **S1** — YouTube search and stream resolution from a plain JVM.
 *
 * This is the gate on risk R1: if NewPipeExtractor cannot extract without a
 * WebView, Phase 1 has to be replanned. Nothing in Phase 1 starts until this
 * class is green.
 *
 * These are integration tests — they hit YouTube over the network — so they
 * are deliberately kept in their own package and selected explicitly in CI
 * (`./gradlew :core:test --tests '*SpikeTest*'`).
 */
class YoutubeExtractionSpikeTest {

    private fun service(): StreamingService {
        NewPipe.init(HttpDownloader(), Localization("en", "US"))
        // getServiceByUrl() only accepts URLs that look like a *stream* link, so
        // a bare homepage fails. Picking by base URL is what we actually mean.
        return NewPipe.getServices()
            .first { it.baseUrl.contains("youtube.com", ignoreCase = true) }
    }

    @Test
    fun `search returns results without a WebView`() {
        val service = service()

        val query = service.searchQHFactory.fromQuery("creative commons music")
        val info = SearchInfo.getInfo(service, query)

        val items = info.relatedItems
        println("S1 search: ${items.size} items, nextPage=${info.hasNextPage()}")
        items.take(5).forEach { println("  - ${it.name} -> ${it.url}") }

        assertTrue(
            items.isNotEmpty(),
            "Search returned no items — YouTube is likely blocking this client " +
                "or the extractor's innertube parsing has drifted",
        )
        assertTrue(
            items.all { !it.url.isNullOrEmpty() },
            "At least one search result had no URL",
        )
    }

    @Test
    fun `stream extraction yields playable http streams`() {
        val service = service()

        val info = StreamInfo.getInfo(service, KNOWN_VIDEO)

        val video = info.videoStreams
        val audio = info.audioStreams
        val muxed = video.filter { it.isUrl }
        val dash = info.dashMpdUrl
        val hls = info.hlsUrl

        println("S1 stream: '${info.name}' by ${info.uploaderName}")
        println("  videoStreams=${video.size} (${muxed.size} with a direct URL)")
        println("  audioStreams=${audio.size}")
        println("  dash=${dash != null}  hls=${hls != null}")
        video.take(5).forEach { println("    v ${it.format?.id} ${it.url?.take(70)}") }
        audio.take(5).forEach { println("    a ${it.format?.id} ${it.url?.take(70)}") }

        assertTrue(
            muxed.isNotEmpty() || audio.isNotEmpty() || dash != null || hls != null,
            "No playable stream of any kind: no muxed URL, no audio streams and " +
                "no DASH/HLS manifest. This is the R1 failure mode.",
        )
        assertTrue(
            audio.isNotEmpty() || dash != null || hls != null,
            "No audio source found — video alone is not enough to play",
        )

        // A URL that resolves but 403s is worse than no URL: it would pass S1 and
        // then fail at playback. Prove the bytes actually flow.
        val probe = audio.firstOrNull { it.isUrl } ?: muxed.firstOrNull()
        if (probe != null) {
            val status = probeBytes(probe.url)
            println("  probe ${probe.format?.id}: HTTP ${status.first}, ${status.second} bytes")
            assertTrue(
                status.first in 200..299,
                "Stream URL returned HTTP ${status.first} — Google is rejecting " +
                    "this client's playback requests (PoToken / S2 territory)",
            )
            assertTrue(status.second > 0, "Stream URL returned no bytes")
        }
    }

    /** Fetches the first kilobyte of [url] and returns (status code, byte count). */
    private fun probeBytes(url: String?): Pair<Int, Long> {
        require(!url.isNullOrBlank()) { "stream URL is null or blank" }
        val request = okhttp3.Request.Builder()
            .url(url)
            .header("Range", "bytes=0-1023")
            .header("User-Agent", HttpDownloader.DEFAULT_USER_AGENT)
            .build()

        HttpDownloader().client.newCall(request).execute().use { response ->
            val bytes = response.body?.bytes()?.size?.toLong() ?: 0L
            return response.code to bytes
        }
    }

    private companion object {
        /** Globally available, never removed, not region locked. */
        const val KNOWN_VIDEO = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    }
}
