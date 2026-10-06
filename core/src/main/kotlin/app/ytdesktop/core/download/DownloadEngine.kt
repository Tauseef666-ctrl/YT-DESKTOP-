/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.download

import app.ytdesktop.core.downloader.HttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/** Raised when a download cannot be completed after retries and recovery. */
class DownloadException(message: String, cause: Throwable? = null) :
    IOException(message, cause)

/**
 * Multi-threaded, resumable HTTP file downloader (plan 2.1).
 *
 * Properties, all asserted by `DownloadEngineTest` offline via MockWebServer:
 *  - **512 KiB blocks**: the file is split into [blockSize] ranges, each
 *    fetched with `Range:` and written at its own offset, so no temporary
 *    re-assembly pass is needed.
 *  - **3 worker threads**: at most [workers] block requests are in flight
 *    (semaphore-gated); blocks are queued in file order.
 *  - **Resume offsets**: an existing destination file is trusted up to the
 *    last whole block — a torn trailing block is truncated, and every block
 *    below that point is skipped instead of re-fetched.
 *  - **403 recovery**: [DownloadRequest.onHttpError] may return a replacement
 *    URL (e.g. a re-extracted stream URL) when a block dies with a hard HTTP
 *    error, and the block retries against it (plan 2.2).
 *  - **No-range servers**: the first block doubles as a range probe; a 200 OK
 *    instead of 206 means the server ignores `Range`, so the single response
 *    body is streamed straight to disk — correct output, no wasted work.
 *
 * Pure JVM, no extractor/Compose/Android dependency: unit-testable offline.
 */
class DownloadEngine(
    private val client: OkHttpClient = HttpDownloader().client,
    private val blockSize: Long = BLOCK_SIZE,
    private val workers: Int = WORKER_THREADS,
    private val maxBlockAttempts: Int = 3,
) {

    /**
     * Fetch [request] to [DownloadRequest.destination], reporting monotonic
     * progress via [onProgress] after every completed block.
     *
     * Throws [DownloadException] on failure; partial data stays on disk so a
     * later call resumes.
     */
    suspend fun download(
        request: DownloadRequest,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadedFile = withContext(Dispatchers.IO) {
        request.destination.parent?.let { Files.createDirectories(it) }

        val total = request.expectedSize ?: headSize(request)
        if (total != null && total <= 0L) {
            Files.write(request.destination, ByteArray(0))
            return@withContext DownloadedFile(request.destination, 0)
        }

        val resumeFrom = alignedResume(request.destination, total)

        if (total == null) {
            // Unknown size: no ranges to divide, so stream sequentially. Still
            // honours UA/headers and 403 recovery, but no resume (a byte count
            // we cannot verify is worse than a clean restart).
            Files.deleteIfExists(request.destination)
            val bytes = streamWhole(request)
            onProgress(DownloadProgress(bytes, bytes, 0))
            return@withContext DownloadedFile(request.destination, bytes)
        }

        if (resumeFrom >= total) {
            onProgress(DownloadProgress(total, total, 0))
            return@withContext DownloadedFile(request.destination, total)
        }

        val ranges = blockRanges(resumeFrom, total)
        val bytesDone = AtomicLong(resumeFrom)
        val activeBlocks = AtomicInteger(0)

        RandomAccessFile(request.destination.toFile(), "rw").use { raf ->
            // Block 0 of the remaining work doubles as the range-support probe:
            // a 206 here means parallel blocks are safe; a 200 means the server
            // ignores Range and its body already IS the whole file.
            val first = ranges.first()
            val probe = fetchBlock(request, first.start, first.end, raf, probe = true)
            when (probe) {
                is BlockWrote -> {
                    bytesDone.addAndGet(probe.written)
                    onProgress(DownloadProgress(bytesDone.get(), total, 0))

                    val rest = ranges.drop(1)
                    if (rest.isNotEmpty()) {
                        parallelBlocks(request, rest, raf, bytesDone, activeBlocks, total, onProgress)
                    }
                }

                is BlockFullBody -> {
                    // The probe was answered with the complete file: raf now
                    // holds every byte (truncated first). Nothing left to fetch.
                    bytesDone.set(probe.written)
                    onProgress(DownloadProgress(probe.written, total, 0))
                }
            }
        }

        val finalSize = Files.size(request.destination)
        if (finalSize != total) {
            throw DownloadException("Incomplete download: got $finalSize of $total bytes")
        }
        DownloadedFile(request.destination, finalSize)
    }

    // ----- internals -------------------------------------------------------

    private suspend fun parallelBlocks(
        request: DownloadRequest,
        ranges: List<BlockRange>,
        raf: RandomAccessFile,
        bytesDone: AtomicLong,
        activeBlocks: AtomicInteger,
        total: Long,
        onProgress: (DownloadProgress) -> Unit,
    ) = coroutineScope {
        val gate = Semaphore(workers)
        ranges.map { range ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    activeBlocks.incrementAndGet()
                    try {
                        val outcome = fetchBlock(request, range.start, range.end, raf, probe = false)
                        bytesDone.addAndGet((outcome as BlockWrote).written)
                    } finally {
                        activeBlocks.decrementAndGet()
                        onProgress(DownloadProgress(bytesDone.get(), total, activeBlocks.get()))
                    }
                }
            }
        }.awaitAll()
    }

    private sealed interface BlockOutcome
    private class BlockWrote(val written: Long) : BlockOutcome
    private class BlockFullBody(val written: Long) : BlockOutcome

    /**
     * One ranged GET with retries and 403 recovery. Body bytes are read fully
     * into memory first (bounded by [blockSize] for range replies) and then
     * written under a short lock, because [RandomAccessFile] shares one file
     * pointer across the three workers.
     */
    private suspend fun fetchBlock(
        request: DownloadRequest,
        start: Long,
        end: Long,
        raf: RandomAccessFile,
        probe: Boolean,
    ): BlockOutcome {
        var url = request.url
        var attempt = 0
        while (true) {
            attempt++
            currentCoroutineContext().ensureActive()
            val outcome: BlockOutcome? = try {
                client.newCall(rangeCall(request, url, start, end)).execute().use { response ->
                    when {
                        response.code == 206 -> {
                            val payload = response.body?.bytes()
                                ?: throw DownloadException("Empty body for bytes=$start-$end")
                            val expected = end - start + 1
                            if (payload.size > expected) {
                                throw DownloadException(
                                    "Range ignored mid-file: got ${payload.size} > $expected bytes",
                                )
                            }
                            synchronized(raf) {
                                raf.seek(start)
                                raf.write(payload)
                            }
                            BlockWrote(payload.size.toLong())
                        }

                        response.code == 200 && probe -> {
                            // Server ignores Range entirely: this body is the
                            // whole file. Replace whatever partial we had.
                            raf.setLength(0)
                            BlockFullBody(writeBodyStream(response, raf, base = 0))
                        }

                        response.code == 200 && !probe -> {
                            // Cannot happen after a successful 200 probe, and
                            // writing a full body mid-file would corrupt others.
                            if (attempt < maxBlockAttempts) null
                            else throw DownloadException("Range ignored for bytes=$start-$end")
                        }

                        response.code in HARD_ERRORS -> {
                            val replacement = request.onHttpError(response.code, url)
                            when {
                                replacement != null && replacement != url -> {
                                    url = replacement
                                    null // retry immediately against the fresh URL
                                }
                                attempt < maxBlockAttempts -> null
                                else -> throw DownloadException(
                                    "HTTP ${response.code} for bytes=$start-$end",
                                )
                            }
                        }

                        else -> if (attempt < maxBlockAttempts) null
                        else throw DownloadException("HTTP ${response.code} for bytes=$start-$end")
                    }
                }
            } catch (e: DownloadException) {
                throw e
            } catch (e: IOException) {
                if (attempt >= maxBlockAttempts) {
                    throw DownloadException("Failed after $attempt attempts: bytes=$start-$end", e)
                }
                null
            }
            if (outcome != null) return outcome
        }
    }

    /** GET with a `Range:` header for [start]..[end], plus UA and overrides. */
    private fun rangeCall(request: DownloadRequest, url: String, start: Long, end: Long): Request {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", request.userAgent)
            .header("Range", "bytes=$start-$end")
            .get()
        request.headerOverrides.forEach { (name, value) ->
            if (!name.equals("range", ignoreCase = true)) builder.header(name, value)
        }
        return builder.build()
    }

    /** Streams [response] to [raf] from [base]; returns bytes written. */
    private fun writeBodyStream(response: okhttp3.Response, raf: RandomAccessFile, base: Long): Long {
        var written = 0L
        raf.seek(base)
        response.body?.byteStream()?.use { input ->
            val buffer = ByteArray(COPY_BUFFER)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                raf.write(buffer, 0, n)
                written += n
            }
        }
        return written
    }

    /**
     * Sequential fallback for servers whose size we could not learn (no HEAD
     * answer). Writes a fresh file from position 0.
     */
    private suspend fun streamWhole(request: DownloadRequest): Long {
        var url = request.url
        var attempt = 0
        while (true) {
            attempt++
            currentCoroutineContext().ensureActive()
            val written: Long? = try {
                val builder = Request.Builder()
                    .url(url)
                    .header("User-Agent", request.userAgent)
                    .get()
                request.headerOverrides.forEach { (name, value) ->
                    if (!name.equals("range", ignoreCase = true)) builder.header(name, value)
                }
                client.newCall(builder.build()).execute().use { response ->
                    when {
                        response.code in HARD_ERRORS -> {
                            val replacement = request.onHttpError(response.code, url)
                            when {
                                replacement != null && replacement != url -> {
                                    url = replacement
                                    null
                                }
                                attempt < maxBlockAttempts -> null
                                else -> throw DownloadException("HTTP ${response.code} fetching $url")
                            }
                        }

                        response.code == 200 || response.code == 206 -> {
                            RandomAccessFile(request.destination.toFile(), "rw").use { raf ->
                                raf.setLength(0)
                                writeBodyStream(response, raf, base = 0)
                            }
                        }

                        else -> if (attempt < maxBlockAttempts) null
                        else throw DownloadException("HTTP ${response.code} fetching $url")
                    }
                }
            } catch (e: DownloadException) {
                throw e
            } catch (e: IOException) {
                if (attempt >= maxBlockAttempts) {
                    throw DownloadException("Failed after $attempt attempts: $url", e)
                }
                null
            }
            if (written != null) return written
        }
    }

    /** HEAD probe for [DownloadRequest.expectedSize]; `null` if unavailable. */
    private fun headSize(request: DownloadRequest): Long? {
        val builder = Request.Builder()
            .url(request.url)
            .header("User-Agent", request.userAgent)
            .method("HEAD", null)
        request.headerOverrides.forEach { (name, value) ->
            if (!name.equals("range", ignoreCase = true)) builder.header(name, value)
        }
        return try {
            client.newCall(builder.build()).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull()
                    ?.takeIf { response.code in 200..299 }
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * How many bytes of [destination] are trustworthy: everything below the
     * last whole [blockSize] boundary, capped at [total]. A torn trailing
     * block (the usual crash case) is truncated away.
     */
    private fun alignedResume(destination: Path, total: Long?): Long {
        if (!Files.exists(destination)) return 0
        val existing = Files.size(destination)
        val whole = existing / blockSize * blockSize
        val aligned = if (total != null) minOf(whole, total) else 0
        if (aligned < existing) {
            RandomAccessFile(destination.toFile(), "rw").use { it.setLength(aligned) }
        }
        return aligned
    }

    private data class BlockRange(val start: Long, val end: Long)

    private fun blockRanges(from: Long, total: Long): List<BlockRange> {
        val ranges = ArrayList<BlockRange>(((total - from) / blockSize + 1).toInt())
        var start = from
        while (start < total) {
            ranges += BlockRange(start, minOf(start + blockSize, total) - 1)
            start += blockSize
        }
        return ranges
    }

    companion object {
        const val BLOCK_SIZE: Long = 512L * 1024
        const val WORKER_THREADS: Int = 3
        private const val COPY_BUFFER = 64 * 1024
        private val HARD_ERRORS = setOf(400, 403, 404, 410, 416)
    }
}