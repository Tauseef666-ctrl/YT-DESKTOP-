/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import net.newpipe.app.theme.spaceLarge
import net.newpipe.app.theme.spaceNormal

/**
 * A titled horizontal rail. Pair with a `SectionHeader` automatically; pass [onSeeAll] to show
 * the "See all" action.
 *
 * ```kotlin
 * VideoRail(title = "Trending", onSeeAll = {}) {
 *     items(videos) { video -> VideoCard(title = video.title, durationText = video.duration) }
 * }
 * ```
 */
@Composable
fun VideoRail(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAll: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = spaceLarge),
    itemSpacing: Dp = spaceNormal,
    content: LazyListScope.() -> Unit
) {
    Column(modifier = modifier) {
        SectionHeader(
            title = title,
            actionLabel = if (onSeeAll != null) "See all" else null,
            onAction = onSeeAll
        )
        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content
        )
    }
}
