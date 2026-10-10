/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Singleton

/**
 * iOS does not host the desktop floating mini-player. Reported unsupported so the UI never shows
 * a dead control.
 */
@Singleton(binds = [PopOutCoordinator::class])
class IOSPopOutCoordinator : PopOutCoordinator {
    override val supported: Boolean = false
    override val isOpen get() = UnsupportedMiniPlayerState.isOpen
    override fun toggle() = Unit

    private object UnsupportedMiniPlayerState {
        val isOpen = MutableStateFlow(false)
    }
}