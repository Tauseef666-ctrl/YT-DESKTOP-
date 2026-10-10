/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShortcutReferenceTest {

    private fun labelFor(key: Key): String =
        Shortcut(PlayerAction.PLAY_PAUSE, key).keyLabel()

    @Test
    fun `blank query returns every default row`() {
        val rows = ShortcutReference.rows("")
        assertEquals(PlayerShortcuts.defaults.size, rows.size)
    }

    @Test
    fun `whitespace query is treated as blank`() {
        assertEquals(PlayerShortcuts.defaults.size, ShortcutReference.rows("   ").size)
    }

    @Test
    fun `query matches action label`() {
        val rows = ShortcutReference.rows("mute")
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.actionLabel == PlayerAction.MUTE.label })
    }

    @Test
    fun `query matches description`() {
        val actions = ShortcutReference.rows("item").map { it.actionLabel }.toSet()
        assertTrue(PlayerAction.NEXT.label in actions)
        assertTrue(PlayerAction.PREVIOUS.label in actions)
        assertEquals(2, actions.size)
    }

    @Test
    fun `query matches human-readable key label`() {
        val rows = ShortcutReference.rows("space")
        assertEquals(1, rows.size)
        assertEquals(PlayerAction.PLAY_PAUSE.label, rows.single().actionLabel)
        assertEquals("Space", rows.single().keyLabel)
    }

    @Test
    fun `query is case-insensitive and trimmed`() {
        val rows = ShortcutReference.rows("  MUTE  ")
        assertEquals(2, rows.size)
        assertTrue(rows.all { it.actionLabel == PlayerAction.MUTE.label })
    }

    @Test
    fun `unknown query returns no rows`() {
        assertTrue(ShortcutReference.rows("zzz").isEmpty())
    }

    @Test
    fun `key labels are human readable`() {
        assertEquals("Space", labelFor(Key.Spacebar))
        assertEquals("\u2192", labelFor(Key.DirectionRight))
        assertEquals("\u2190", labelFor(Key.DirectionLeft))
        assertEquals("Media Next", labelFor(Key.MediaNext))
        assertEquals("Media Play/Pause", labelFor(Key.MediaPlayPause))
        assertEquals("Volume Mute", labelFor(Key.VolumeMute))
        assertEquals("M", labelFor(Key.M))
        assertEquals("S", labelFor(Key.S))
    }

    @Test
    fun `reserved key labels cover media and volume keys`() {
        val reserved = ShortcutReference.reservedKeyLabels()
        listOf(
            "Media Play/Pause",
            "Media Stop",
            "Media Next",
            "Media Previous",
            "Volume Up",
            "Volume Down",
            "Volume Mute"
        ).forEach { assertTrue(it in reserved, "missing reserved label: $it") }
        assertEquals(PlayerShortcuts.reservedMediaKeys.size, reserved.size)
    }
}
