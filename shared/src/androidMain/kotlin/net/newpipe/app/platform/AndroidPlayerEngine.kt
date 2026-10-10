/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import net.newpipe.app.player.UnavailablePlayerEngine
import org.koin.core.annotation.Singleton

/**
 * Android has no shared playback engine yet — the existing `:app` ExoPlayer stack still owns
 * playback. This binding keeps the shared graph complete and reports honestly as unavailable.
 */
@Singleton(binds = [net.newpipe.app.player.PlayerEngine::class])
class AndroidPlayerEngine :
    UnavailablePlayerEngine("Shared playback engine is not available on Android yet.")
