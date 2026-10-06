/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ytdesktop.core.model.PagedResult
import app.ytdesktop.core.model.PageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The paging + state holder behind every screen that shows a list of
 * `PageItem`s (plan.md 1.7). Owns loading/error/item accumulation and exposes
 * a Compose-observable snapshot read by [FeedColumn].
 */
class ResourceFeed(
    private val scope: CoroutineScope,
    private val loadFirst: suspend () -> PagedResult<PageItem>,
) {
    var items by mutableStateOf<List<PageItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var current: PagedResult<PageItem>? = null

    val isExhausted: Boolean get() = current?.isExhausted ?: true

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                current = loadFirst()
                items = current?.items.orEmpty()
            } catch (e: Exception) {
                error = e.message ?: "Failed to load"
                items = emptyList()
            } finally {
                loading = false
            }
        }
    }

    fun loadMore() {
        if (loading || current?.isExhausted != false) return
        scope.launch {
            loading = true
            try {
                val more = current?.loadMore()
                if (more != null) {
                    current = more
                    items = items + more.items
                }
            } catch (e: Exception) {
                error = e.message ?: "Failed to load more"
            } finally {
                loading = false
            }
        }
    }
}