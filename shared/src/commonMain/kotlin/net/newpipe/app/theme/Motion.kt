/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Whether non-essential motion should be suppressed (accessibility "reduce motion" preference).
 * Provided by the app once the appearance/privacy settings are wired; defaults to false.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/** True when the user has asked to reduce motion. */
@Composable
fun plusReducedMotion(): Boolean = LocalReducedMotion.current

/** Returns [millis] unless reduced motion is active, in which case it returns `0`. */
@Composable
fun plusMotionMillis(millis: Int): Int = if (plusReducedMotion()) 0 else millis

/**
 * Picks [animated] normally, or [reduced] when the reduced-motion preference is active.
 */
@Composable
fun <T> plusMotion(animated: T, reduced: T): T = if (plusReducedMotion()) reduced else animated
