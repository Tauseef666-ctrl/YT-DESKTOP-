/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType

/**
 * Platform-independent view of a key event. Compose exposes different accessor surfaces per
 * platform, so the platform layer normalizes them into this value class before resolution.
 */
data class KeyEventInfo(
    val type: KeyEventType = KeyEventType.Unknown,
    val key: Key = Key.Unknown,
    val isCtrlPressed: Boolean = false,
    val isShiftPressed: Boolean = false,
    val isAltPressed: Boolean = false,
    val isMetaPressed: Boolean = false
)

/**
 * Intercepts key events for the focused composition. Returns `true` from [onKey] to consume the
 * event (text fields and other controls will not see it again).
 */
expect fun Modifier.onPlayerKeyEvent(onKey: (KeyEventInfo) -> Boolean): Modifier