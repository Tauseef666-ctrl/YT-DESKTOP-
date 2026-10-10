/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import net.newpipe.app.player.UnavailablePlayerEngine
import org.koin.core.annotation.Singleton

/**
 * iOS has no playback engine wired up yet; this binding keeps the shared graph complete and
 * reports honestly as unavailable rather than faking playback.
 */
@Singleton(binds = [net.newpipe.app.player.PlayerEngine::class])
class IOSPlayerEngine :
    UnavailablePlayerEngine("Playback is not available on iOS yet.")
