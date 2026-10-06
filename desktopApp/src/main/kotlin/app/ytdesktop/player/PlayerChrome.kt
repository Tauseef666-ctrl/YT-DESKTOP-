/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.player.PlayerEngine

/**
 * The player chrome (plan.md 1.6): now-playing line, a tap-to-seek progress
 * bar, and the transport row that drives [engine] through its contract.
 */
@Composable
fun PlayerChrome(engine: PlayerEngine, modifier: Modifier = Modifier) {
    // The getter reads VlcPlayer's snapshot fields inside composition scope, so
    // this line alone re-runs the chrome whenever playback state changes.
    val state = engine.state

    Column(modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(
            state.currentTitle ?: "Nothing playing — pick a video from a feed",
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

        state.error?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

internal fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}