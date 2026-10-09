/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.download

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Plan 2.4 — the download queue. Fully offline like [DownloadEngineTest]: the
 * manager downloads from a range-aware MockWebServer and every lifecycle
 * property (completion, bounded concurrency, pause/resume, cancel, retry) is
 * asserted without touching the network.
 */
class DownloadManagerTest {

    // ----- completion ------------------------------------------------------

    @Test
    fun `queued jobs complete to disk`() = withServer(payload(BLOCK * 2 + 5)) { server, dir ->
        val manager = DownloadManager()
        val jobs = (1..2).map {
            manager.enqueue(
                title = "job $it",
                parts = listOf(
                    DownloadPart("video", request(server, "/f$it", dir, "v$it", payloadSize)),
                ),
            )
        }

        runBlocking { await(manager) { it.size == 2 && it.all { j -> j.state == DownloadJobState.Completed } } }

        jobs.forEachIndexed { i, id ->
            val done = runBlocking { manager.jobs.value.first { it.id == id } }
            assertEquals(DownloadJobState.Completed, done.state)
            assertEquals(payloadSize, done.bytesDone, "job $i must report the full size")
            assertContentEquals(payload(payloadSize), Files.readAllBytes(dir.resolve("v${i + 1}")))
        }
    }

    @Test
    fun `at most three jobs are in flight at once`() = withServer(payload(BLOCK)) { server, dir ->
        server.dispatcher = SlowDispatcher(payload(BLOCK), sleepMillis = 250)
        val manager = DownloadManager()
        (1..4).forEach {
            manager.enqueue("job $it", listOf(DownloadPart("v", request(server, "/f$it", dir, "p$it", BLOCK))))
        }

        runBlocking { await(manager) { it.size == 4 && it.all { j -> j.state == DownloadJobState.Completed } } }

        val dispatcher = server.dispatcher as SlowDispatcher
        assertTrue(dispatcher.maxInFlight.get() <= DownloadManager.MAX_CONCURRENT_JOBS, "queue must cap concurrent jobs")
        assertTrue(dispatcher.maxInFlight.get() >= 2, "jobs must actually overlap")
    }

    // ----- pause / resume ---------------------------------------------------

    @Test
    fun `pause keeps partials and resume completes from the resume point`() = withServer(payload(BLOCK * 4)) { server, dir ->
        server.dispatcher = SlowDispatcher(payload(BLOCK * 4), sleepMillis = 300)
        val manager = DownloadManager()
        val id = manager.enqueue("paused", listOf(DownloadPart("v", request(server, "/f", dir, "p", BLOCK * 4))))

        runBlocking {
            await(manager) { j ->
                j.firstOrNull { it.id == id }?.let { it.state == DownloadJobState.Downloading && it.bytesDone > 0 } == true
            }
            manager.pause(id)
            await(manager) { it.first { j -> j.id == id }.state == DownloadJobState.Paused }
        }
        val partial = dir.resolve("p")
        assertTrue(Files.exists(partial), "pause must keep the partial file")
        assertTrue(Files.size(partial) > 0)

        runBlocking { manager.resume(id) }

        runBlocking { await(manager) { it.first { j -> j.id == id }.state == DownloadJobState.Completed } }
        assertContentEquals(payload(BLOCK * 4), Files.readAllBytes(partial))
    }

    @Test
    fun `cancel drops the job and deletes partials`() = withServer(payload(BLOCK * 4)) { server, dir ->
        server.dispatcher = SlowDispatcher(payload(BLOCK * 4), sleepMillis = 300)
        val manager = DownloadManager()
        val id = manager.enqueue("cancelled", listOf(DownloadPart("v", request(server, "/f", dir, "p", BLOCK * 4))))

        runBlocking {
            await(manager) { it.any { j -> j.id == id && j.state == DownloadJobState.Downloading } }
        }
        runBlocking { manager.cancel(id) }
        runBlocking { await(manager) { it.none { j -> j.id == id } } }
        assertFalse(Files.exists(dir.resolve("p")), "cancel must delete the partial file")
    }

    // ----- failure / retry ---------------------------------------------------

    @Test
    fun `hard error fails the job then retry recovers`() = withServer(payload(BLOCK + 40)) { server, dir ->
        server.dispatcher = FlakyDispatcher(payload(BLOCK + 40), failGets = 10)
        val manager = DownloadManager()
        val id = manager.enqueue("flaky", listOf(DownloadPart("v", request(server, "/f", dir, "p", BLOCK + 40))))

        runBlocking { await(manager) { it.first { j -> j.id == id }.state == DownloadJobState.Failed } }
        val failed = runBlocking { manager.jobs.value.first { it.id == id } }
        assertTrue(failed.error?.contains("HTTP") == true, "failure must carry the HTTP error, got ${failed.error}")

        (server.dispatcher as FlakyDispatcher).allowSuccess()
        runBlocking { manager.resume(id) }

        runBlocking { await(manager) { it.first { j -> j.id == id }.state == DownloadJobState.Completed } }
        assertContentEquals(payload(BLOCK + 40), Files.readAllBytes(dir.resolve("p")))
    }

    // ----- harness -----------------------------------------------------------

    private fun request(
        server: MockWebServer,
        path: String,
        dir: Path,
        name: String,
        size: Long,
    ) = DownloadRequest(
        url = server.url(path).toString(),
        destination = dir.resolve(name),
        expectedSize = size,
    )

    private fun withServer(
        payload: ByteArray,
        block: (MockWebServer, Path) -> Unit,
    ) {
        val server = MockWebServer().apply { dispatcher = RangedDispatcher(payload); start() }
        val dir = Files.createTempDirectory("ytds-dl-manager-test")
        try {
            block(server, dir)
        } finally {
            server.shutdown()
            dir.toFile().deleteRecursively()
        }
    }

    private suspend fun await(
        manager: DownloadManager,
        timeoutMs: Long = 15_000,
        predicate: (List<DownloadJob>) -> Boolean,
    ) = withTimeout(timeoutMs) {
        while (!predicate(manager.jobs.value)) delay(20)
    }

    /** Counts overlapping GETs across all jobs (job-level concurrency probe). */
    private class SlowDispatcher(
        payload: ByteArray,
        private val sleepMillis: Long,
    ) : RangedDispatcher(payload) {
        val maxInFlight = AtomicInteger(0)
        private val inFlight = AtomicInteger(0)

        override fun dispatch(request: RecordedRequest): MockResponse {
            val current = inFlight.incrementAndGet()
            maxInFlight.accumulateAndGet(current, Math::max)
            try {
                Thread.sleep(sleepMillis)
                return super.dispatch(request)
            } finally {
                inFlight.decrementAndGet()
            }
        }
    }

    /** Answers the first run of GETs with 500, then behaves normally. */
    private class FlakyDispatcher(
        payload: ByteArray,
        private val failGets: Int,
    ) : RangedDispatcher(payload) {
        @Volatile
        private var remaining = failGets

        override fun dispatch(request: RecordedRequest): MockResponse {
            if (request.method == "GET" && remaining > 0) {
                synchronized(this) { remaining-- }
                return MockResponse().setResponseCode(500)
            }
            return super.dispatch(request)
        }

        fun allowSuccess() {
            synchronized(this) { remaining = 0 }
        }
    }

    private open class RangedDispatcher(private val payload: ByteArray) : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
            val range = request.getHeader("Range") ?: return MockResponse().setResponseCode(200)
                .setHeader("Content-Length", payload.size)
                .setBody(Buffer().write(payload))
            val (from, to) = range.removePrefix("bytes=").split("-")
            val start = from.toLong()
            val clippedEnd = minOf(to.toLong(), payload.size - 1L)
            val slice = payload.copyOfRange(start.toInt(), clippedEnd.toInt() + 1)
            return MockResponse().setResponseCode(206)
                .setHeader("Content-Range", "bytes $start-$clippedEnd/${payload.size}")
                .setHeader("Accept-Ranges", "bytes")
                .setBody(Buffer().write(slice))
        }
    }

    private fun payload(size: Long) = ByteArray(size.toInt()) { i -> (i * 31 + 7).toByte() }

    private companion object {
        const val BLOCK = DownloadEngine.BLOCK_SIZE
        val payloadSize = (BLOCK * 2 + 5)
    }
}