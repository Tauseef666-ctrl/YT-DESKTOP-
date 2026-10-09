/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.StreamingService
import app.ytdesktop.ui.common.Toolbar
import app.ytdesktop.ui.feed.FeedColumn
import app.ytdesktop.ui.feed.ResourceFeed
import kotlinx.coroutines.delay

/** Search with live suggestions (debounced), recent-queries, and a paging feed. */
@Composable
fun SearchScreen(
    service: StreamingService,
    onVideoClick: (StreamItem) -> Unit,
    modifier: Modifier = Modifier,
    history: List<String> = emptyList(),
    onQuerySubmitted: (String) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onChannelClick: (String) -> Unit = {},
    onDownload: ((StreamItem) -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf<String?>(null) }
    var hasFocus by remember { mutableStateOf(false) }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }

    val feed = remember(submitted, scope, service) {
        ResourceFeed(scope) { service.search(requireNotNull(submitted)) }
    }
    LaunchedEffect(submitted) { if (submitted != null) feed.refresh() }

    LaunchedEffect(query, hasFocus, submitted) {
        if (query.isBlank() || !hasFocus || submitted != null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        suggestions = runCatching { service.suggestions(query) }.getOrDefault(emptyList())
    }

    fun submit(value: String) {
        if (value.isBlank()) return
        suggestions = emptyList()
        submitted = value
        onQuerySubmitted(value)
    }

    Column(modifier) {
        Toolbar(title = "Search")
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .onFocusChanged { hasFocus = it.isFocused },
            placeholder = { Text("Search YouTube") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit(query) }),
        )

        when {
            suggestions.isNotEmpty() -> {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(suggestions, key = { it }) { suggestion ->
                        Text(
                            suggestion,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = suggestion; submit(suggestion) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            submitted == null && query.isBlank() && history.isNotEmpty() -> {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Recent searches",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Clear",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onClearHistory),
                    )
                }
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(history, key = { it }) { term ->
                        Text(
                            term,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { query = term; submit(term) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            submitted == null -> {
                Text(
                    "Search for videos, then press Enter",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                )
            }

            else -> {
                Row(Modifier.fillMaxWidth()) {
                    FeedColumn(
                        feed = feed,
                        onVideoClick = onVideoClick,
                        onChannelClick = onChannelClick,
                        onDownload = onDownload,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}