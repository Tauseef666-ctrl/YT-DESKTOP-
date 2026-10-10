/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Koin module for shared, platform-independent playback infrastructure (e.g. the
 * [PlaybackController]). Platform engines live in `net.newpipe.app.platform`.
 */
@Module
@ComponentScan
@Configuration
object PlayerModule
