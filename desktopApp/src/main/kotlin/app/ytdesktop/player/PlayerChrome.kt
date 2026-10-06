/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.player.PlayerEngine

/**
 * The player chrome (plan.md 1.6/1.7): now-playing line, a tap-to-seek progress
 * bar, the transport row, and — when a queue is attached — up-next navigation
 * with a jumpable queue list.
 */
@Composable
fun PlayerChrome(
    engine: PlayerEngine,
    modifier: Modifier = Modifier,
    queue: List<StreamItem> = emptyList(),
    currentIndex: Int = -1,
    resolving: Boolean = false,
    onSelectQueue: (Int) -> Unit = {},
    onNext: (() -> Unit)? = null,
    onPrevious: (() -> Unit)? = null,
) {
    // The getter reads VlcPlayer's snapshot fields inside composition scope, so
    // this line alone re-runs the chrome whenever playback state changes.
    val state = engine.state

    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            state.currentTitle ?: (if (resolving) "Preparing…" else "Nothing playing — pick a video from a feed"),
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .pointerInput(state) {
                    detectTapGestures { offset ->
                        val width = size.width.toFloat()
                        if (width > 0f) engine.seekTo(offset.x / width)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            LinearProgressIndicator(
                progress = { state.positionFraction },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Text(
            "${formatTimeMs(state.timeMs)} / ${formatTimeMs(state.lengthMs)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { engine.togglePlayPause() }, enabled = state.hasMedia) {
                Text(if (state.isPlaying) "Pause" else "Play")
            }
            OutlinedButton(onClick = { engine.stop() }, enabled = state.hasMedia) { Text("Stop") }
            OutlinedButton(onClick = { engine.skip(-10) }, enabled = state.hasMedia) { Text("-10s") }
            OutlinedButton(onClick = { engine.skip(10) }, enabled = state.hasMedia) { Text("+10s") }
        }

        if (queue.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Queue (${queue.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (onPrevious != null) {
                    OutlinedButton(onClick = onPrevious, enabled = currentIndex > 0) { Text("↑ Prev") }
                }
                if (onNext != null) {
                    OutlinedButton(onClick = onNext, enabled = currentIndex in 0 until queue.lastIndex) {
                        Text(if (resolving) "…" else "Next ↓")
                    }
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                queue.take(QUEUE_PREVIEW).forEachIndexed { index, item ->
                    val isCurrent = index == currentIndex
                    Text(
                        item.title,
                        style = if (isCurrent) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.bodySmall,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clickable(enabled = !isCurrent) { onSelectQueue(index) }
                            .padding(vertical = 2.dp),
                    )
                }
                if (queue.size > QUEUE_PREVIEW) {
                    Text(
                        "… and ${queue.size - QUEUE_PREVIEW} more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        state.error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private const val QUEUE_PREVIEW = 8

internal fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}