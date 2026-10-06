/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.EmbeddedMediaPlayerComponent

/**
 * Owns one [EmbeddedMediaPlayerComponent] and mirrors its state into Compose.
 *
 * The component is created lazily on first composition so that
 * `BundledVlc.configure()` (which must run before any VLCJ class loads) always
 * gets there first.
 *
 * Events arrive on libVLC's own thread; snapshot state is written from there,
 * which is safe.
 */
@Stable
class VlcPlayer {

    private var component: EmbeddedMediaPlayerComponent? = null

    var isPlaying by mutableStateOf(false)
        private set
    var timeMs by mutableLongStateOf(0L)
        private set
    var lengthMs by mutableLongStateOf(0L)
        private set
    var position by mutableFloatStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val listener = object : MediaPlayerEventAdapter() {
        override fun playing(player: MediaPlayer) {
            isPlaying = true
        }

        override fun paused(player: MediaPlayer) {
            isPlaying = false
        }

        override fun stopped(player: MediaPlayer) {
            isPlaying = false
            position = 0f
            timeMs = 0L
        }

        override fun finished(player: MediaPlayer) {
            isPlaying = false
        }

        override fun error(player: MediaPlayer) {
            isPlaying = false
            error = "Playback failed — the stream could not be decoded or the URL was rejected"
        }

        override fun timeChanged(player: MediaPlayer, time: Long) {
            timeMs = time
        }

        override fun lengthChanged(player: MediaPlayer, length: Long) {
            lengthMs = length
        }

        override fun positionChanged(player: MediaPlayer, position: Float) {
            this@VlcPlayer.position = position
        }
    }

    /** The Swing host that libVLC renders into. Created once, then reused. */
    internal fun swingComponent(): EmbeddedMediaPlayerComponent {
        component?.let { return it }
        val created = EmbeddedMediaPlayerComponent()
        created.mediaPlayer().events().addMediaPlayerEventListener(listener)
        component = created
        return created
    }

    fun play(mrl: String) {
        error = null
        swingComponent().mediaPlayer().media().play(mrl)
    }

    fun togglePlayPause() {
        val player = swingComponent().mediaPlayer()
        if (player.status().isPlaying) player.controls().setPause(true) else player.controls().play()
    }

    fun stop() {
        swingComponent().mediaPlayer().controls().stop()
    }

    fun seekTo(fraction: Float) {
        swingComponent().mediaPlayer().controls().setPosition(fraction.coerceIn(0f, 1f))
    }

    fun skipSeconds(seconds: Long) {
        swingComponent().mediaPlayer().controls().skipTime(seconds * 1000L)
    }

    fun release() {
        component?.release()
        component = null
    }
}

@Composable
fun rememberVlcPlayer(): VlcPlayer = remember { VlcPlayer() }

/**
 * The video surface itself: a black stage with libVLC painting into a Swing
 * component hosted by Compose.
 *
 * Deliberately *no* controls are stacked on top of the stage. Compose Desktop
 * puts a SwingPanel above the Skia layer, so a Compose overlay would be drawn
 * underneath the video. Controls therefore live in their own row below the
 * stage; whether an overlay can be made to work is an open question for spike
 * S3.
 */
@Composable
fun VlcVideoSurface(
    player: VlcPlayer,
    modifier: Modifier = Modifier,
) {
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(
        modifier
            .fillMaxWidth()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        SwingPanel(
            factory = { player.swingComponent() },
            modifier = Modifier.fillMaxSize(),
        )
        if (player.error != null) {
            Text(
                player.error!!,
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

/**
 * Phase 0.4 harness: the surface plus just enough control to drive spike S3.
 * Replaced by the real player chrome in Phase 1 (task 1.6).
 */
@Composable
fun VlcPlayerHarness(player: VlcPlayer, modifier: Modifier = Modifier) {
    var mrl by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            VlcVideoSurface(player, Modifier.fillMaxSize())
        }

        Column(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BasicTextField(
                value = mrl,
                onValueChange = { mrl = it },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (mrl.isEmpty()) {
                        Text(
                            "https://… stream URL or local file path",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    inner()
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (mrl.isNotBlank()) player.play(mrl.trim()) },
                    enabled = mrl.isNotBlank(),
                ) {
                    Text("Play")
                }
                OutlinedButton(onClick = { player.togglePlayPause() }) {
                    Text(if (player.isPlaying) "Pause" else "Resume")
                }
                OutlinedButton(onClick = { player.stop() }) {
                    Text("Stop")
                }
                OutlinedButton(onClick = { player.skipSeconds(-10) }) { Text("-10s") }
                OutlinedButton(onClick = { player.skipSeconds(10) }) { Text("+10s") }
            }

            Text(
                progressLabel(player),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun progressLabel(player: VlcPlayer): String {
    if (player.lengthMs <= 0L) return "Idle"
    val position = (player.position * 100).toInt()
    return "${formatTime(player.timeMs)} / ${formatTime(player.lengthMs)}  ($position%)"
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
