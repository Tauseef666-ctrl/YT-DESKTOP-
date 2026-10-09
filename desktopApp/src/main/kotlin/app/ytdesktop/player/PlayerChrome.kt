/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.player.PlayerEngine

// NewPipe-style player panel: the controls are always black regardless of the
// surrounding theme, accent is NewPipe red (#FF5252).
private val PlayerPanel = Color(0xFF111111)
private val PlayerForeground = Color(0xFFFFFFFF)
private val PlayerMuted = Color(0xFFBDBDBD)
private val PlayerAccent = Color(0xFFFF5252)

/**
 * The player chrome (plan.md 1.6/1.7), NewPipe-style: now-playing title,
 * red seek slider with time labels, circular transport buttons, a LIVE badge
 * for broadcasts (length stays 0 on live manifests), and — when a queue is
 * attached — up-next navigation with a jumpable queue list.
 *
 * Note: VLC renders into a heavyweight Swing component, so Chrome lives below
 * the video stage rather than overlaying it (see VlcVideoSurface docs).
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

    // Live broadcasts keep lengthMs at 0; having a title without any known
    // length is the signal to show the red LIVE badge and keep long-only times.
    val live = state.currentTitle != null && state.lengthMs <= 0L
    val hasMedia = state.hasMedia || state.currentTitle != null

    Column(
        modifier
            .fillMaxWidth()
            .background(PlayerPanel)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                state.currentTitle
                    ?: if (resolving) "Preparing…"
                    else "Nothing playing — pick a video from a feed",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (state.currentTitle == null) PlayerMuted else PlayerForeground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (live) {
                Text(
                    "● LIVE",
                    fontSize = 11.sp,
                    color = PlayerAccent,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Slider(
            value = state.positionFraction.coerceIn(0f, 1f),
            onValueChange = { engine.seekTo(it) },
            enabled = hasMedia,
            colors = SliderDefaults.colors(
                thumbColor = PlayerAccent,
                activeTrackColor = PlayerAccent,
                inactiveTrackColor = Color(0xFF424242),
            ),
            modifier = Modifier.padding(top = 2.dp),
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                formatTimeMs(state.timeMs),
                fontSize = 12.sp,
                color = PlayerMuted,
            )
            Text(
                if (live) "broadcast" else formatTimeMs(state.lengthMs),
                fontSize = 12.sp,
                color = PlayerMuted,
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TransportButton("−10", onClick = { engine.skip(-10) }, enabled = hasMedia)
            TransportButton(
                if (state.isPlaying) "❚❚" else "▶",
                onClick = { engine.togglePlayPause() },
                enabled = hasMedia,
                prominent = true,
                size = 46.dp,
            )
            TransportButton("+10", onClick = { engine.skip(10) }, enabled = hasMedia)
            TransportButton("■", onClick = { engine.stop() }, enabled = hasMedia)
        }

        if (queue.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Up next",
                    fontSize = 12.sp,
                    color = PlayerMuted,
                )
                if (onPrevious != null) {
                    Text(
                        "↑ Prev",
                        fontSize = 12.sp,
                        color = if (currentIndex > 0) PlayerAccent else PlayerMuted,
                        modifier = Modifier
                            .clickable(enabled = currentIndex > 0) { onPrevious() }
                            .padding(2.dp),
                    )
                }
                if (onNext != null) {
                    Text(
                        if (resolving) "…" else "Next ↓",
                        fontSize = 12.sp,
                        color = if (currentIndex in 0 until queue.lastIndex) PlayerAccent else PlayerMuted,
                        modifier = Modifier
                            .clickable(enabled = currentIndex in 0 until queue.lastIndex) { onNext() }
                            .padding(2.dp),
                    )
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                queue.take(QUEUE_PREVIEW).forEachIndexed { index, item ->
                    val isCurrent = index == currentIndex
                    Text(
                        item.title,
                        fontSize = 12.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        color = when {
                            isCurrent -> PlayerAccent
                            else -> PlayerMuted
                        },
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
                        fontSize = 12.sp,
                        color = PlayerMuted,
                    )
                }
            }
        }

        state.error?.let {
            Text(
                it,
                fontSize = 12.sp,
                color = PlayerAccent,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TransportButton(
    symbol: String,
    onClick: () -> Unit,
    enabled: Boolean,
    prominent: Boolean = false,
    size: Dp = 38.dp,
) {
    Box(
        Modifier
            .size(size)
            .background(
                if (prominent) PlayerAccent else Color(0x1FFFFFFF),
                CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol,
            fontSize = if (prominent) 16.sp else 13.sp,
            color = if (prominent) Color(0xFF000000) else Color(0xFFFFFFFF),
            fontWeight = FontWeight.Bold,
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