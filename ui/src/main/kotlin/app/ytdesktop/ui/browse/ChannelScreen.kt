/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.browse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.errors.ErrorInfo
import app.ytdesktop.core.errors.UserAction
import app.ytdesktop.core.errors.YtException
import app.ytdesktop.core.model.ChannelPage
import app.ytdesktop.core.model.ChannelTab
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.StreamingService
import app.ytdesktop.ui.common.SurfaceChip
import app.ytdesktop.ui.common.Toolbar
import app.ytdesktop.ui.errors.ErrorCard
import app.ytdesktop.ui.feed.FeedColumn
import app.ytdesktop.ui.feed.ResourceFeed
import app.ytdesktop.ui.feed.compactCount
import coil3.compose.AsyncImage
import kotlinx.coroutines.CoroutineScope

/**
 * Channel page (plan.md 1.5): header, tab chips, and each playable tab's
 * self-paging feed. `channelInfo`/`channelTab` are slice-A service methods;
 * this is their screen.
 */
@Composable
fun ChannelScreen(
    service: StreamingService,
    channelUrl: String,
    onBack: () -> Unit,
    onVideoClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    onChannelClick: (String) -> Unit = {},
    onDownload: ((StreamItem) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var page by remember(channelUrl) { mutableStateOf<ChannelPage?>(null) }
    var channelError by remember(channelUrl) { mutableStateOf<ErrorInfo?>(null) }
    var selectedTab by remember(channelUrl) { mutableStateOf<ChannelTab?>(null) }
    var retryKey by remember(channelUrl) { mutableStateOf(0) }

    LaunchedEffect(channelUrl, retryKey) {
        channelError = null
        selectedTab = null
        runCatching { service.channelInfo(channelUrl) }
            .onSuccess { info ->
                page = info
                info.tabs.firstOrNull()?.let { selectedTab = it }
            }
            .onFailure { e ->
                page = null
                channelError = (e as? YtException)?.errorInfo
                    ?: ErrorInfo(UserAction.CHANNEL, "Could not load this channel")
            }
    }

    Column(modifier) {
        Toolbar(
            title = page?.title ?: "Channel",
            leading = {
                TextButton(onClick = onBack) { Text("‹ Back") }
            },
        )

        val info = page
        when {
            channelError != null -> {
                ErrorCard(
                    info = channelError!!,
                    onRetry = { retryKey++ },
                    modifier = Modifier.padding(16.dp),
                )
            }

            info != null -> {
                ChannelHeader(info)
                TabChips(
                    tabs = info.tabs,
                    selectedUrl = selectedTab?.url,
                    onSelect = { selectedTab = it },
                )
                val tab = selectedTab
                if (tab == null) {
                    Text(
                        "No playable tabs on this channel",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    FeedColumn(
                        feed = rememberFeed(scope = scope, tabUrl = tab.url, service = service),
                        onVideoClick = onVideoClick,
                        onChannelClick = onChannelClick,
                        onDownload = onDownload,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                }
            }

            else -> {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun rememberFeed(
    scope: CoroutineScope,
    tabUrl: String,
    service: StreamingService,
): ResourceFeed = remember(tabUrl, scope, service) {
    ResourceFeed(scope) { service.channelTab(tabUrl) }
}.also { feed ->
    LaunchedEffect(tabUrl) { feed.refresh() }
}

@Composable
private fun ChannelHeader(page: ChannelPage) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        if (page.avatarUrl != null) {
            AsyncImage(
                model = page.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .padding(1.dp),
            )
            Spacer(Modifier.width(16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(page.title, style = MaterialTheme.typography.titleMedium)
            page.subscriberCount?.let {
                Text(
                    "${compactCount(it)} subscribers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val description = page.description
            if (!description.isNullOrBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TabChips(
    tabs: List<ChannelTab>,
    selectedUrl: String?,
    onSelect: (ChannelTab) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabs.forEach { tab ->
            SurfaceChip(
                label = tab.name,
                selected = selectedUrl == tab.url,
                onClick = { onSelect(tab) },
            )
        }
    }
}