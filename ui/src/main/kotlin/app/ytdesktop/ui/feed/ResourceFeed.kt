/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.ytdesktop.core.errors.ErrorInfo
import app.ytdesktop.core.errors.UserAction
import app.ytdesktop.core.errors.YtException
import app.ytdesktop.core.model.PagedResult
import app.ytdesktop.core.model.PageItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The paging + state holder behind every screen that shows a list of
 * `PageItem`s (plan.md 1.7). Owns loading/error/item accumulation and exposes
 * a Compose-observable snapshot read by [FeedColumn]. Errors keep their
 * core [ErrorInfo] taxonomy so surfaces can render action + message + retry.
 */
class ResourceFeed(
    private val scope: CoroutineScope,
    private val loadFirst: suspend () -> PagedResult<PageItem>,
) {
    var items by mutableStateOf<List<PageItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<ErrorInfo?>(null)
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
                error = toErrorInfo(UserAction.UNKNOWN, e)
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
                error = toErrorInfo(UserAction.UNKNOWN, e)
            } finally {
                loading = false
            }
        }
    }

    private fun toErrorInfo(fallback: UserAction, e: Exception): ErrorInfo =
        (e as? YtException)?.errorInfo ?: ErrorInfo(fallback, e.message ?: "Unexpected error")
}