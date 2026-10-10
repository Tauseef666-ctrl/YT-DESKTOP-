/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import net.newpipe.app.platform.JVMPlayerEngine
import java.awt.Canvas
import java.awt.Color

/**
 * Embeds the native libVLC video surface into the Compose window. Only meaningful when [engine] is
 * the JVM VLCJ engine; any other engine renders nothing here.
 */
@Composable
actual fun PlayerVideoSurface(engine: PlayerEngine, modifier: Modifier) {
    val jvmEngine = engine as? JVMPlayerEngine
        ?: return
    SwingPanel(
        factory = {
            Canvas().apply {
                background = Color.BLACK
                jvmEngine.attachVideoSurface(this)
            }
        },
        modifier = modifier
    )
}
