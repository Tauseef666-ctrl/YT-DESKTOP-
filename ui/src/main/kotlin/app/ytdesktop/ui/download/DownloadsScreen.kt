/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.download

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ytdesktop.core.download.DownloadJob
import app.ytdesktop.core.download.DownloadJobState
import app.ytdesktop.core.download.DownloadManager
import app.ytdesktop.ui.common.Toolbar
import java.nio.file.Path

/**
 * The download queue (plan.md 2.4): one row per job with live progress,
 * pause/resume/cancel/retry, and a shortcut to the files on disk.
 */
@Composable
fun DownloadsScreen(
    manager: DownloadManager,
    onOpenFolder: (Path) -> Unit,
    modifier: Modifier = Modifier,
    notice: String? = null,
) {
    val jobs by manager.jobs.collectAsState()
    val hasCompleted = jobs.any { it.state == DownloadJobState.Completed }
    val hasFailed = jobs.any { it.state == DownloadJobState.Failed }

    Column(modifier) {
        Toolbar(
            title = "Downloads",
            trailing = {
                if (hasFailed) {
                    TextButton(onClick = manager::retryAll) { Text("Retry all") }
                }
                if (hasCompleted) {
                    TextButton(onClick = manager::clearCompleted) { Text("Clear completed") }
                }
            },
        )

        if (notice != null) {
            Text(
                notice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (jobs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No downloads yet — use the ↓ button on any video",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(jobs, key = { it.id }) { job ->
                    DownloadRow(
                        job = job,
                        onPause = { manager.pause(job.id) },
                        onResume = { manager.resume(job.id) },
                        onCancel = { manager.cancel(job.id) },
                        onOpenFolder = onOpenFolder,
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadRow(
    job: DownloadJob,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onOpenFolder: (Path) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            job.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))

        val total = job.bytesTotal
        val fraction = if (total != null && total > 0) (job.bytesDone.toFloat() / total) else null
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
        } else {
            LinearProgressIndicator(Modifier.fillMaxWidth().height(6.dp))
        }

        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stateLabel(job),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            when (job.state) {
                DownloadJobState.Queued, DownloadJobState.Downloading -> {
                    TextButton(onClick = onPause) { Text("Pause") }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                DownloadJobState.Paused, DownloadJobState.Failed -> {
                    TextButton(onClick = onResume) { Text(if (job.state == DownloadJobState.Failed) "Retry" else "Resume") }
                    TextButton(onClick = onCancel) { Text("Cancel") }
                }
                DownloadJobState.Completed -> {
                    job.parts.firstOrNull()?.file?.parent?.let { folder ->
                        TextButton(onClick = { onOpenFolder(folder) }) { Text("Open folder") }
                    }
                }
            }
        }
    }
}

private fun stateLabel(job: DownloadJob): String = when (job.state) {
    DownloadJobState.Queued -> "Queued"
    DownloadJobState.Downloading ->
        "${formatBytes(job.bytesDone)}${job.bytesTotal?.let { " / ${formatBytes(it)}" } ?: ""}"
    DownloadJobState.Paused -> "Paused"
    DownloadJobState.Completed -> "Completed · ${formatBytes(job.bytesDone)}"
    DownloadJobState.Failed -> job.error ?: "Failed"
}

private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes ${units[0]}" else "%.1f %s".format(value, units[unit])
}