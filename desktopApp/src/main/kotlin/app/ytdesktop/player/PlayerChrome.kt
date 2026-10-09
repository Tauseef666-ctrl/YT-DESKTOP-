/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.player.PlayerEngine

private val PlayerBg = Color(0xFF0C0C0E)
private val PlayerAccent = Color(0xFFFF5252)
private val PlayerText = Color(0xFFF2F2F4)
private val PlayerDim = Color(0xFFA8A8AF)
private val PlayerRing = Color(0x33FFFFFF)

/**
 * The NewPipe-style player panel: a black control stage with a now-playing
 * line, a draggable seek bar with running time, and a circular transport row
 * (play/pause centered, ±10s, stop, fullscreen). When a queue is attached it
 * adds up-next navigation.
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
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
) {
    // Reading the engine snapshot inside composition re-runs the chrome on every
    // playback-state change (position, play/pause, length, error).
    val state = engine.state

    Column(
        modifier
            .fillMaxWidth()
            .background(PlayerBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            state.currentTitle
                ?: (if (resolving) "Preparing…" else "Nothing playing — pick a video or open your Library"),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = PlayerText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formatTimeMs(state.timeMs), color = PlayerDim, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Slider(
                value = state.positionFraction.coerceIn(0f, 1f),
                onValueChange = { engine.seekTo(it) },
                enabled = state.hasMedia,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = PlayerAccent,
                    activeTrackColor = PlayerAccent,
                    inactiveTrackColor = PlayerRing,
                ),
            )
            Spacer(Modifier.width(8.dp))
            Text(formatTimeMs(state.lengthMs), color = PlayerDim, fontSize = 12.sp)
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            CircleButton("−10", enabled = state.hasMedia, onClick = { engine.skip(-10) })
            CircleButton(
                if (state.isPlaying) "❚❚" else "▶",
                primary = true,
                large = true,
                enabled = state.hasMedia,
                onClick = { engine.togglePlayPause() },
            )
            CircleButton("+10", enabled = state.hasMedia, onClick = { engine.skip(10) })
            CircleButton("■", enabled = state.hasMedia, onClick = { engine.stop() })
            Spacer(Modifier.weight(1f))
            CircleButton(if (isFullscreen) "⤡" else "⛶", onClick = onToggleFullscreen)
        }

        if (queue.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Queue (${queue.size})", color = PlayerDim, fontSize = 12.sp)
                if (onPrevious != null) {
                    CircleButton("↑", enabled = currentIndex > 0, onClick = onPrevious)
                }
                if (onNext != null) {
                    CircleButton(
                        if (resolving) "…" else "↓",
                        enabled = currentIndex in 0 until queue.lastIndex,
                        onClick = onNext,
                    )
                }
            }
            Column(Modifier.fillMaxWidth()) {
                queue.take(QUEUE_PREVIEW).forEachIndexed { index, item ->
                    val isCurrent = index == currentIndex
                    Text(
                        item.title,
                        style = if (isCurrent) MaterialTheme.typography.labelMedium
                        else MaterialTheme.typography.bodySmall,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isCurrent) PlayerAccent else PlayerDim,
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
                        color = PlayerDim,
                    )
                }
            }
        }

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF6E6E))
        }
    }
}

@Composable
private fun CircleButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
    large: Boolean = false,
) {
    val diameter = if (large) 54.dp else 42.dp
    val base = Modifier
        .size(diameter)
        .clip(CircleShape)
        .then(
            if (primary) Modifier.background(PlayerAccent)
            else Modifier.border(1.dp, PlayerRing, CircleShape),
        )
        .clickable(enabled = enabled, onClick = onClick)
    Box(base, contentAlignment = Alignment.Center) {
        Text(
            label,
            color = if (enabled) PlayerText else Color(0x55FFFFFF),
            fontSize = if (large) 20.sp else 15.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private const val QUEUE_PREVIEW = 8

internal fun formatTimeMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}