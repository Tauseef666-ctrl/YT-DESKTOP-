/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.newpipe.app.theme.plusColors

/**
 * Desktop application shell: an optional navigation rail on the left, an optional toolbar and
 * content column, and an optional bottom bar (e.g. a mini-player).
 *
 * @param rail Sidebar slot, usually a [NavRail].
 * @param toolbar Top bar slot, usually a [TopToolbar].
 * @param bottomBar Persistent bottom slot (mini-player bar, status).
 * @param content Main content, filling the remaining space.
 */
@Composable
fun PlusScaffold(
    modifier: Modifier = Modifier,
    rail: (@Composable () -> Unit)? = null,
    toolbar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = plusColors()
    Row(modifier = modifier.fillMaxSize().background(colors.background)) {
        if (rail != null) {
            rail()
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(colors.border)
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            if (toolbar != null) {
                toolbar()
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider)
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
            if (bottomBar != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.divider)
                )
                bottomBar()
            }
        }
    }
}
