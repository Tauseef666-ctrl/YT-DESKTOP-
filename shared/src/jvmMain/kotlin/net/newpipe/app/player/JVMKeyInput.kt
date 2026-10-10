/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

actual fun Modifier.onPlayerKeyEvent(onKey: (KeyEventInfo) -> Boolean): Modifier =
    onPreviewKeyEvent { event ->
        onKey(
            KeyEventInfo(
                type = event.type,
                key = event.key,
                isCtrlPressed = event.isCtrlPressed,
                isShiftPressed = event.isShiftPressed,
                isAltPressed = event.isAltPressed,
                isMetaPressed = event.isMetaPressed
            )
        )
    }