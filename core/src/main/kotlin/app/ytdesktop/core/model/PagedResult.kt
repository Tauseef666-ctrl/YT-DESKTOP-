/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.model

/**
 * A page of browsable items with an optional continuation loader (see
 * plan.md 1.7 — every list keeps its "next page" strategy encapsulated).
 */
class PagedResult<out T>(
    val items: List<T>,
    private val nextLoader: (suspend () -> PagedResult<T>)?,
) {
    val isExhausted: Boolean get() = nextLoader == null

    suspend fun loadMore(): PagedResult<T>? = nextLoader?.invoke()

    companion object {
        fun <T> exhausted(items: List<T>): PagedResult<T> = PagedResult(items, null)
    }
}