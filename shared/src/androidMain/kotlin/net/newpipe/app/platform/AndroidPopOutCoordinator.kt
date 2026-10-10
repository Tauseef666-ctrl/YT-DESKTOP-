/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Singleton

/**
 * Android platform does not surface the desktop floating mini-player yet (Picture-in-Picture is
 * handled by the OS). Reported unsupported so the UI never shows a dead control.
 */
@Singleton(binds = [PopOutCoordinator::class])
class AndroidPopOutCoordinator : PopOutCoordinator {
    override val supported: Boolean = false
    override val isOpen get() = UnsupportedMiniPlayerState.isOpen
    override fun toggle() = Unit

    private object UnsupportedMiniPlayerState {
        val isOpen = MutableStateFlow(false)
    }
}