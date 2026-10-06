/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.browse

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.StreamingService
import app.ytdesktop.ui.feed.FeedColumn
import app.ytdesktop.ui.feed.ResourceFeed
import kotlinx.coroutines.CoroutineScope

/**
 * Trending kiosk browser (plan.md 1.3). The kiosk list is discovered at
 * runtime; selecting a chip swaps the feed, and the feed pages itself.
 */
@Composable
fun TrendingScreen(
    service: StreamingService,
    onVideoClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val kiosks by produceState<List<String>?>(null) {
        value = runCatching { service.availableKiosks }.getOrNull()
    }
    var selectedKiosk by remember { mutableStateOf<String?>(null) }

    Column(modifier) {
        KioskChips(kiosks = kiosks, selected = selectedKiosk, onSelect = { selectedKiosk = it })
        FeedColumn(
            feed = rememberFeed(scope = scope, kioskId = selectedKiosk, service = service),
            onVideoClick = onVideoClick,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}

@Composable
private fun rememberFeed(
    scope: CoroutineScope,
    kioskId: String?,
    service: StreamingService,
): ResourceFeed = remember(kioskId, scope, service) {
    ResourceFeed(scope) { service.trending(kioskId) }
}.also { feed ->
    androidx.compose.runtime.LaunchedEffect(kioskId) { feed.refresh() }
}

@Composable
private fun KioskChips(
    kiosks: List<String>?,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(label = "Trending", selected = selected == null, onClick = { onSelect(null) })
        kiosks?.forEach { id ->
            Chip(
                label = prettify(id),
                selected = selected == id,
                onClick = { onSelect(id) },
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/** "trending_music" -> "Music", "live" -> "Live". */
private fun prettify(id: String): String = id
    .split('_')
    .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    .ifBlank { "Trending" }