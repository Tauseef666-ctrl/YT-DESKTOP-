/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Hosts the engine's video output inside the shared player frame.
 *
 * Desktop/JVM embeds the native libVLC surface (AWT component via the engine). Platforms without a
 * real video sink render an empty surface so layout stays stable — they never draw fake video.
 */
@Composable
expect fun PlayerVideoSurface(engine: PlayerEngine, modifier: Modifier = Modifier)
