/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.spike

import app.ytdesktop.core.service.StreamingService
import app.ytdesktop.core.service.YoutubeService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Live-stream regression gate (user-reported: "live feed and live videos are
 * not playing"). A live broadcast must resolve to ONE self-contained HLS/DASH
 * manifest with no split audio-component, because our DASH pairing never
 * plays live. Network test -> CI `spikes` job, matching `*SpikeTest*`.
 *
 * Video ids used here are long-running, but if either is ever delisted this
 * test fails loudly and the id just needs bumping.
 */
class YoutubeLiveSpikeTest {

    private val service: StreamingService = YoutubeService()

    @Test
    fun `live stream resolves to a standalone manifest, no split audio`() = runBlocking {
        val resolved = service.resolvePlayback("https://www.youtube.com/watch?v=nA9UZF-SZoQ")

        assertNull(resolved.audio, "live must not expose a separate audio stream")
        assertTrue(
            resolved.video.url.contains("/file/index."),
            "live must hand VLC a manifest URL, got ${resolved.video.url}",
        )
        assertTrue(resolved.title.isNotBlank())
    }

    @Test
    fun `recorded video still splits into video plus audio`() = runBlocking {
        val resolved = service.resolvePlayback("https://www.youtube.com/watch?v=dQw4w9WgXcQ")

        assertNotNull(resolved.audio, "recorded videos keep a DASH audio component")
        assertFalse(resolved.video.url.contains("/file/index."), "recorded videos must not be manifests")
    }
}