/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic NewPipe+ color tokens: a premium dark, cinematic palette with a restrained red
 * accent. Provided through [LocalPlusColors] by [AppTheme] so components read tokens instead
 * of hard-coding Material3 roles. Material3 roles stay available and remain the source for
 * existing screens.
 */
data class PlusColors(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceSunken: Color,
    val border: Color,
    val divider: Color,
    val accent: Color,
    val onAccent: Color,
    val accentPressed: Color,
    val accentMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val overlayScrim: Color
)

val DarkPlusColors = PlusColors(
    background = Color(0xFF0A0A0B),
    surface = Color(0xFF0F0F12),
    surfaceElevated = Color(0xFF17171C),
    surfaceSunken = Color(0xFF070708),
    border = Color(0xFF2A2A33),
    divider = Color(0xFF1E1E25),
    accent = Color(0xFFE53935),
    onAccent = Color(0xFFFFFFFF),
    accentPressed = Color(0xFFB71C1C),
    accentMuted = Color(0xFF3A1416),
    textPrimary = Color(0xFFF2F2F5),
    textSecondary = Color(0xFFA8A8B3),
    textMuted = Color(0xFF6E6E7A),
    success = Color(0xFF4CAF50),
    warning = Color(0xFFFFB300),
    danger = Color(0xFFEF5350),
    overlayScrim = Color(0xCC000000)
)

val LightPlusColors = PlusColors(
    background = Color(0xFFF7F7F9),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEFEFF3),
    border = Color(0xFFD9D9E0),
    divider = Color(0xFFE6E6EC),
    accent = Color(0xFFCD201F),
    onAccent = Color(0xFFFFFFFF),
    accentPressed = Color(0xFF9E1918),
    accentMuted = Color(0xFFFFDAD6),
    textPrimary = Color(0xFF1A1A1E),
    textSecondary = Color(0xFF55555F),
    textMuted = Color(0xFF8A8A94),
    success = Color(0xFF2E7D32),
    warning = Color(0xFFEF6C00),
    danger = Color(0xFFC62828),
    overlayScrim = Color(0x66000000)
)

/** The active NewPipe+ token set. Defaults to dark so previews and non-themed callers are stable. */
val LocalPlusColors = staticCompositionLocalOf { DarkPlusColors }

/** Convenience accessor for the active [PlusColors]. */
@Composable
fun plusColors(): PlusColors = LocalPlusColors.current
