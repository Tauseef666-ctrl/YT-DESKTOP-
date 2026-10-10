/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.newpipe.app.theme.plusColors

private val SnackbarShape = RoundedCornerShape(10.dp)

/**
 * Snackbar host styled with the NewPipe+ tokens. Pair with a `remember { SnackbarHostState() }`
 * (or hoist one into the shell) and call `hostState.showSnackbar(...)`.
 */
@Composable
fun PlusSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val colors = plusColors()
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = SnackbarShape,
            containerColor = colors.surfaceElevated,
            contentColor = colors.textPrimary,
            actionColor = colors.accent
        )
    }
}
