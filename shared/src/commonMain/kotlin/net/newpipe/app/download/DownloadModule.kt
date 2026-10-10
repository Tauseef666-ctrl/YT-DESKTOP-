/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.download

import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Configuration
import org.koin.core.annotation.Module

/**
 * Koin module for the shared download infrastructure (engine, store). Implementations live in
 * platform packages, this covers the shared declarations.
 */
@Module
@ComponentScan
@Configuration
object DownloadModule