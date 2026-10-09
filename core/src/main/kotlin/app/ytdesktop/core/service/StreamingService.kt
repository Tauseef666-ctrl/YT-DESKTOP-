/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.service

import app.ytdesktop.core.model.ChannelPage
import app.ytdesktop.core.model.PagedResult
import app.ytdesktop.core.model.PageItem
import app.ytdesktop.core.model.ResolvedDownload
import app.ytdesktop.core.model.ResolvedPlayback
import app.ytdesktop.core.model.StreamDetails

/**
 * The pluggable service seam (plan.md 1.1). The UI layer depends only on this
 * interface, so a future second front-end provider slots in without touching
 * any view code.
 */
interface StreamingService {
    val name: String
    val baseUrl: String

    /** Ids of the service's browsable kiosks (e.g. trending, music, news). */
    val availableKiosks: List<String>

    /** Full-text search. `query` is the raw, unencoded user query. */
    suspend fun search(query: String): PagedResult<PageItem>

    /** Search-as-you-type suggestions for [query]. */
    suspend fun suggestions(query: String): List<String>

    /** A kiosk feed; [kioskId] null means the service default (trending). */
    suspend fun trending(kioskId: String?): PagedResult<PageItem>

    /** Channel header + tabs for a channel/playlist URL. */
    suspend fun channelInfo(url: String): ChannelPage

    /** One paged feed of a channel tab (videos, streams, shorts, playlists…). */
    suspend fun channelTab(url: String): PagedResult<PageItem>

    /** Picks the best playable video-only + audio pair for [videoUrl] (1.6/R7). */
    suspend fun resolvePlayback(videoUrl: String): ResolvedPlayback

    /**
     * Picks the best downloadable video-only + audio pair for [videoUrl]
     * (2.4). Same R7 AV1 avoidance as playback; live broadcasts are rejected.
     */
    suspend fun resolveDownload(videoUrl: String): ResolvedDownload

    /** Full video page metadata including related content (1.4/1.5). */
    suspend fun streamDetails(url: String): StreamDetails
}