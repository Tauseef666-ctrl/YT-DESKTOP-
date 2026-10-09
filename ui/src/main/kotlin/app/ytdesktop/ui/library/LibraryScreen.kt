/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.library.LocalMedia
import java.nio.file.Path

/**
 * Local library: lets the user point the app at a folder of videos they already
 * downloaded (e.g. with NewPipe) and plays any of them in the built-in player.
 */
@Composable
fun LibraryScreen(
    folder: Path?,
    media: List<LocalMedia>,
    onChooseFolder: () -> Unit,
    onRefresh: () -> Unit,
    onPlay: (LocalMedia) -> Unit,
    modifier: Modifier = Modifier,
    scanning: Boolean = false,
) {
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Library", style = MaterialTheme.typography.headlineSmall)
                Text(
                    folder?.toString() ?: "No folder selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            if (folder != null) {
                OutlinedButton(onClick = onRefresh, enabled = !scanning) { Text("Refresh") }
                Spacer(Modifier.width(8.dp))
            }
            Button(onClick = onChooseFolder) { Text("Choose folder…") }
        }

        when {
            folder == null -> Centered(
                "Choose a folder to play videos you have downloaded — for example NewPipe's download folder.",
            )
            scanning -> Centered("Scanning…")
            media.isEmpty() -> Centered("No playable media found in this folder.")
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(media, key = { it.file.toString() }) { item ->
                    LocalMediaRow(item, onClick = { onPlay(item) })
                }
            }
        }
    }
}

@Composable
private fun LocalMediaRow(item: LocalMedia, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("▶", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(formatSize(item.sizeBytes))
                    if (item.subtitle != null) append("  ·  subtitles")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Centered(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "unknown size"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit++
    }
    return if (unit == 0) "$bytes ${units[0]}" else "%.1f %s".format(value, units[unit])
}