/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.download

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicLong

/** Lifecycle of one queued download (plan.md 2.4). */
enum class DownloadJobState {
    Queued,
    Downloading,
    Paused,
    Completed,
    Failed,
}

/** Progress of one stream inside a job (video part, audio part). */
data class PartProgress(
    val label: String,
    val bytesDone: Long,
    val bytesTotal: Long?,
    val file: Path,
)

/** UI-visible snapshot of a queued download. */
data class DownloadJob(
    val id: Long,
    val title: String,
    val state: DownloadJobState,
    val error: String?,
    val parts: List<PartProgress>,
) {
    val bytesDone: Long get() = parts.sumOf { it.bytesDone }
    val bytesTotal: Long?
        get() = parts.mapNotNull { it.bytesTotal }.takeIf { it.size == parts.size }?.sum()
    val active: Boolean
        get() = state == DownloadJobState.Queued || state == DownloadJobState.Downloading
}

/** One stream to fetch as part of a [DownloadJob]. */
data class DownloadPart(
    val label: String,
    val request: DownloadRequest,
)

/**
 * The download queue (plan.md 2.4). At most [DownloadManager.MAX_CONCURRENT_JOBS]
 * jobs download at the same time; each job's parts run sequentially (the
 * engine already parallelizes a file's blocks). Resuming rides on the engine's
 * resume: pausing cancels the current part's fetch but keeps the partial file,
 * and a later run skips the blocks already on disk.
 *
 * Pure JVM and offline-testable — `DownloadManagerTest` drives it against a
 * range-aware MockWebServer, exactly like the engine tests.
 */
class DownloadManager(
    private val engine: DownloadEngine = DownloadEngine(),
    private val scope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _jobs = MutableStateFlow<List<DownloadJob>>(emptyList())
    val jobs: StateFlow<List<DownloadJob>> = _jobs.asStateFlow()

    private val nextId = AtomicLong(1)
    private val gate = Semaphore(MAX_CONCURRENT_JOBS)

    /** Insertion-ordered map: enqueue order is display order. */
    private val active = LinkedHashMap<Long, ActiveJob>()

    /** Queues [parts] for [title] and returns the new job's id. */
    fun enqueue(title: String, parts: List<DownloadPart>): Long {
        val id = nextId.getAndIncrement()
        val job = ActiveJob(id, title, parts.map { ActivePart(it.label, it.request) })
        synchronized(active) {
            active[id] = job
            snapshot()
        }
        dispatch(job)
        return id
    }

    /** Requests a short pause (partials kept on disk; resumes later). */
    fun pause(id: Long) {
        val job = activeJob(id) ?: return
        if (!job.current().active) return
        synchronized(job) { job.pausedByUser = true }
        job.handle?.cancel()
        job.set(DownloadJobState.Paused)
    }

    /** Re-queues a paused or failed job; blocks already on disk are skipped. */
    fun resume(id: Long) {
        val job = activeJob(id) ?: return
        synchronized(job) {
            job.pausedByUser = false
            job.state = DownloadJobState.Queued
            job.error = null
        }
        snapshot()
        dispatch(job)
    }

    /** Stops [id] and deletes any partial files from disk. */
    fun cancel(id: Long) {
        val job = activeJob(id) ?: return
        synchronized(job) { job.pausedByUser = false }
        val handle = job.handle
        val cleanup = {
            runCatching { job.removePartialFiles() }
            synchronized(active) {
                active.remove(id)
                snapshot()
            }
        }
        if (handle == null) {
            cleanup()
        } else {
            // Delete only once the worker has released the file handle (the
            // engine may be mid-write when we cancel, and Windows locks it).
            handle.cancel()
            handle.invokeOnCompletion { cleanup() }
        }
    }

    /** Drops completed entries (finished files remain on disk). */
    fun clearCompleted() {
        synchronized(active) {
            active.entries.removeIf { it.value.current().state == DownloadJobState.Completed }
            snapshot()
        }
    }

    /** Re-queues every failed job (used by a "retry all" affordance). */
    fun retryAll() {
        val failed = synchronized(active) {
            active.values.filter { it.current().state == DownloadJobState.Failed }
        }
        failed.forEach { resume(it.id) }
    }

    private fun activeJob(id: Long): ActiveJob? = synchronized(active) { active[id] }

    private fun dispatch(job: ActiveJob) {
        scope.launch {
            gate.withPermit { runJob(job) }
        }.also { job.handle = it }
    }

    private suspend fun runJob(job: ActiveJob) {
        for (part in job.parts) {
            if (part.done()) continue
            job.set(DownloadJobState.Downloading)
            try {
                val file = engine.download(part.request) { p ->
                    synchronized(job) {
                        part.bytesDone = p.bytesDone
                        part.bytesTotal = p.totalBytes
                    }
                    snapshot()
                }
                synchronized(job) {
                    part.bytesDone = file.bytes
                    part.bytesTotal = file.bytes
                }
                snapshot()
            } catch (e: CancellationException) {
                // A user pause keeps the job and its partials; anything else
                // (cancel() or app teardown) drops the job quietly.
                if (synchronized(job) { job.pausedByUser }) {
                    job.set(DownloadJobState.Paused)
                    return
                }
                throw e
            } catch (e: Exception) {
                val message = (e as? DownloadException)?.message ?: (e.message ?: "Download failed")
                job.set(DownloadJobState.Failed, message)
                return
            }
        }
        job.set(DownloadJobState.Completed)
    }

    private fun snapshot() = synchronized(active) {
        _jobs.update { active.values.map { it.current() } }
    }

    /** Mutable worker-side state; the public face is the [DownloadJob] snapshot. */
    private inner class ActiveJob(
        val id: Long,
        val title: String,
        val parts: List<ActivePart>,
    ) {
        var state: DownloadJobState = DownloadJobState.Queued
        var error: String? = null
        var handle: Job? = null
        var pausedByUser = false

        fun current(): DownloadJob = DownloadJob(
            id = id,
            title = title,
            state = state,
            error = error,
            parts = parts.map { it.toProgress() },
        )

        fun set(state: DownloadJobState, error: String? = null) {
            synchronized(this) {
                this.state = state
                this.error = error
            }
            snapshot()
        }

        fun removePartialFiles() {
            parts.filterNot { it.done() }.forEach { Files.deleteIfExists(it.request.destination) }
        }
    }

    private inner class ActivePart(
        val label: String,
        val request: DownloadRequest,
    ) {
        @Volatile
        var bytesDone: Long = 0

        @Volatile
        var bytesTotal: Long? = null

        fun done(): Boolean {
            val total = bytesTotal ?: return false
            return total > 0 && bytesDone >= total
        }

        fun toProgress() = PartProgress(label, bytesDone, bytesTotal, request.destination)
    }

    companion object {
        const val MAX_CONCURRENT_JOBS = 3
    }
}