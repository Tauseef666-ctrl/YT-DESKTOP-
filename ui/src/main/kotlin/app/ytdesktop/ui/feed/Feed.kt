/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ytdesktop.core.model.PageItem
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.ui.errors.ErrorCard
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * The scrolling feed of [PageItem]s used by Trending, Search and channel tabs.
 * Reaches the end of the current page before asking [ResourceFeed] to load the
 * next.
 */
@Composable
fun FeedColumn(
    feed: ResourceFeed,
    onVideoClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    onChannelClick: (String) -> Unit = {},
) {
    val listState = rememberLazyListState()

    LaunchedEffect(feed) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: Int.MAX_VALUE
        }.distinctUntilChanged().collect { lastVisible ->
            if (feed.items.isNotEmpty() && lastVisible >= feed.items.lastIndex - 4) {
                feed.loadMore()
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items(feed.items, key = { it.url }) { item ->
            when (item) {
                is PageItem.Video -> VideoCard(item.item, onClick = { onVideoClick(item.item) })
                is PageItem.Playlist -> PageCard(
                    title = item.title,
                    subtitle = "Playlist · ${item.uploaderName}",
                    thumbnailUrl = item.thumbnailUrl,
                )
                is PageItem.Channel -> PageCard(
                    title = item.title,
                    subtitle = item.subscriberCount?.let { "Channel · ${compactCount(it)}" } ?: "Channel",
                    thumbnailUrl = item.thumbnailUrl,
                    onClick = { onChannelClick(item.url) },
                )
            }
        }

        when {
            feed.loading -> item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
            feed.error != null -> item {
                ErrorCard(
                    info = feed.error!!,
                    onRetry = if (feed.items.isEmpty()) feed::refresh else feed::loadMore,
                )
            }
            feed.items.isEmpty() && !feed.loading -> item {
                Text(
                    "Nothing here yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/**
 * NewPipe-style stream row (list_stream_item.xml): a flat list entry — no card
 * background — with a left thumbnail whose duration badge overlays its
 * bottom-right corner, a 2-line title, the uploader, and a "views" detail line.
 */
@Composable
fun VideoCard(item: StreamItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Box {
            AsyncImage(
                model = item.thumbnailUrl,
                contentDescription = item.title,
                modifier = Modifier
                    .width(CARD_THUMB_WIDTH)
                    .height(CARD_THUMB_HEIGHT)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            if (item.durationSeconds > 0) {
                Text(
                    formatDuration(item.durationSeconds),
                    color = DurationTextColor,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .background(DurationBadgeColor)
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.uploaderName,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.viewCount > 0) {
                Text(
                    "${compactCount(item.viewCount)} views",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Non-playable rows (playlists and channels) render a lighter card. */
@Composable
private fun PageCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 4.dp, vertical = 8.dp),
    ) {
        Box {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = title,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val CARD_THUMB_WIDTH = 200.dp
private val CARD_THUMB_HEIGHT = 113.dp

// NewPipe list: duration badge (duration_background_color #AA000000,
// duration_text_color #EEFFFFFF) bottom-right of the thumbnail.
private val DurationBadgeColor = Color(0xAA000000)
private val DurationTextColor = Color(0xEEFFFFFF)

/** "1234" -> "1.2K", "1520000" -> "1.5M". */
internal fun compactCount(value: Long): String = when {
    value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
    value >= 1_000 -> "%.1fK".format(value / 1_000.0)
    else -> value.toString()
}

internal fun formatDuration(seconds: Long): String {
    val s = seconds % 60
    val m = (seconds / 60) % 60
    val h = seconds / 3600
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}