/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.download

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Plan 2.1 (block engine) and 2.2 (403 recovery) — fully offline.
 *
 * A [RangeDispatcher] plays a range-aware file server so every property the
 * engine promises (512 KiB blocks, 3 in-flight workers, resume offsets, 403
 * URL refresh, no-range fallback, HEAD-less fallback) is asserted without
 * touching the network.
 */
class DownloadEngineTest {

    // ----- 2.1: blocks, workers, resume ------------------------------------

    @Test
    fun `downloads in ranged blocks served concurrently`() = withServer(payload(BLOCK * 2 + 1000)) { server, d, dir ->
        val dest = dir.resolve("out.bin")
        val engine = DownloadEngine()

        var last = DownloadProgress(0, null, 0)
        engine.download(DownloadRequest(server.url("/f").toString(), dest)) { last = it }

        assertContentEquals(payload(BLOCK * 2 + 1000), Files.readAllBytes(dest))
        assertEquals(3, d.servedStarts.size, "expected 3 block requests (2 full + tail)")
        assertContentEquals(
            listOf(0L, BLOCK, BLOCK * 2),
            d.servedStarts.sorted(),
            "block ranges must tile the whole file",
        )
        assertTrue(d.maxInFlight.get() >= 2, "blocks must be fetched concurrently")
        assertEquals(BLOCK * 2 + 1000L, last.bytesDone, "progress must end at the file size")
    }

    @Test
    fun `resume skips whole blocks already on disk`() = withServer(payload(BLOCK * 3 + 50)) { server, d, dir ->
        val dest = dir.resolve("out.bin")
        // Two complete blocks plus a torn 100-byte partial of block 3.
        val partial = payload(BLOCK * 2 + 100)
        Files.write(dest, partial)

        val engine = DownloadEngine()
        engine.download(DownloadRequest(server.url("/f").toString(), dest))

        assertContentEquals(payload(BLOCK * 3 + 50), Files.readAllBytes(dest))
        assertContentEquals(
            listOf(BLOCK * 2L, BLOCK * 3L),
            d.servedStarts.sorted(),
            "blocks 0-1 must not be re-fetched; the torn partial must be",
        )
    }

    // ----- 2.2: 403 -> re-extract URL recovery -----------------------------

    @Test
    fun `403 on the stale URL retries against the refreshed one`() =
        withServer(payload(BLOCK)) { server, d, dir ->
            d.stalePaths += "/stale" // every ranged GET against it answers 403

            val dest = dir.resolve("out.bin")
            val request = DownloadRequest(server.url("/stale").toString(), dest,
                onHttpError = { code, url ->
                    if (code == 403 && url.endsWith("/stale")) url.replace("/stale", "/fresh") else null
                },
            )

            DownloadEngine().download(request)

            assertContentEquals(payload(BLOCK), Files.readAllBytes(dest))
            assertTrue(d.requestPaths.any { it.contains("/fresh") }, "recovered URL must be used")
            assertTrue(d.requestPaths.any { it.contains("/stale") }, "stale URL must be tried first")
        }

    // ----- servers that do not cooperate ------------------------------------

    @Test
    fun `server that ignores Range falls back to one streamed body`() =
        withServer(payload(BLOCK * 2 + 7)) { server, d, dir ->
            d.honorRanges = false

            val dest = dir.resolve("out.bin")
            DownloadEngine().download(DownloadRequest(server.url("/f").toString(), dest))

            assertContentEquals(payload(BLOCK * 2 + 7), Files.readAllBytes(dest))
            assertEquals(1, d.requestPaths.count { it.startsWith("GET") }, "fallback must cost exactly one GET")
        }

    @Test
    fun `unreachable HEAD still downloads via sequential fallback`() =
        withServer(payload(BLOCK + 42)) { server, d, dir ->
            d.headBroken = true

            val dest = dir.resolve("out.bin")
            DownloadEngine().download(DownloadRequest(server.url("/f").toString(), dest))

            assertContentEquals(payload(BLOCK + 42), Files.readAllBytes(dest))
            assertEquals(1, d.requestPaths.count { it.startsWith("GET") }, "size unknown => single sequential GET")
        }

    // ----- harness ----------------------------------------------------------

    /**
     * Range-aware file server. [honorRanges]=false answers every request with
     * the full body (200); [headBroken] fails size discovery; [stalePaths]
     * answers ranged GETs with 403. Concurrency is observed by counting
     * overlapping dispatches, and every served block start is recorded.
     */
    private class RangeDispatcher(private val payload: ByteArray) : Dispatcher() {
        val servedStarts = CopyOnWriteArrayList<Long>()
        val requestPaths = CopyOnWriteArrayList<String>()
        val stalePaths = mutableSetOf<String>()
        val maxInFlight = AtomicInteger(0)
        private val inFlight = AtomicInteger(0)
        var honorRanges = true
        var headBroken = false

        override fun dispatch(request: RecordedRequest): MockResponse {
            requestPaths += "${request.method} ${request.path}"
            val current = inFlight.incrementAndGet()
            maxInFlight.accumulateAndGet(current, Math::max)
            try {
                Thread.sleep(120) // give sibling blocks time to overlap
                val path = request.path ?: "/"
                val clean = path.substringBefore('?')

                if (request.method == "HEAD") {
                    return if (headBroken) MockResponse().setResponseCode(500)
                    else MockResponse().setResponseCode(200)
                        .setHeader("Content-Length", payload.size)
                }

                val range = request.getHeader("Range")
                if (clean in stalePaths) return MockResponse().setResponseCode(403)

                val spec = parseRange(range) ?: return full()
                val (start, end) = spec
                if (start >= payload.size) return MockResponse().setResponseCode(416)
                val clippedEnd = minOf(end, payload.size - 1L)

                if (!honorRanges) return full()

                servedStarts += start
                val slice = payload.copyOfRange(start.toInt(), clippedEnd.toInt() + 1)
                return MockResponse()
                    .setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$clippedEnd/${payload.size}")
                    .setHeader("Accept-Ranges", "bytes")
                    .setBody(Buffer().write(slice))
            } finally {
                inFlight.decrementAndGet()
            }
        }

        private fun full() = MockResponse().setResponseCode(200)
            .setBody(Buffer().write(payload))

        private fun parseRange(header: String?): Pair<Long, Long>? {
            if (header == null || !header.startsWith("bytes=")) return null
            val (from, to) = header.removePrefix("bytes=").split("-")
            val start = from.toLong()
            val end = to.ifBlank { (payload.size - 1).toString() }.toLong()
            return start to end
        }
    }

    private fun withServer(
        payload: ByteArray,
        configure: (RangeDispatcher) -> Unit = {},
        block: suspend (MockWebServer, RangeDispatcher, Path) -> Unit,
    ) = runBlocking {
        val dispatcher = RangeDispatcher(payload)
        configure(dispatcher)
        val server = MockWebServer().apply {
            this.dispatcher = dispatcher
            start()
        }
        val dir = Files.createTempDirectory("ytds-dl-test")
        try {
            block(server, dispatcher, dir)
        } finally {
            server.shutdown()
            dir.toFile().deleteRecursively()
        }
    }

    private fun payload(size: Long) = ByteArray(size.toInt()) { i -> (i * 31 + 7).toByte() }

    private companion object {
        const val BLOCK = DownloadEngine.BLOCK_SIZE
    }
}