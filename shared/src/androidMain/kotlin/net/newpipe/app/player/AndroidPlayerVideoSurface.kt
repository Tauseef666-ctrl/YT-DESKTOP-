/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Android has no shared video sink yet; renders an empty surface so layout is stable.
 */
@Composable
actual fun PlayerVideoSurface(engine: PlayerEngine, modifier: Modifier) {
    Box(modifier)
}
