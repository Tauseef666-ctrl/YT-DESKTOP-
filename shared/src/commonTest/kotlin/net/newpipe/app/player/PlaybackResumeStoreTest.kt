/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlaybackResumeStoreTest {

    private fun store() = PlaybackResumeStore(MapSettings())

    @Test
    fun saveAndLoadRoundTrip() {
        val s = store()
        s.save("media/1", 12_345L, 120_000L)
        assertEquals(12_345L, s.load("media/1"))
    }

    @Test
    fun positionsNearTheEndAreClearedInsteadOfStored() {
        val s = store()
        s.save("media/1", 115_000L, 120_000L)
        assertEquals(0L, s.load("media/1"))
    }

    @Test
    fun zeroOrNegativePositionsAreIgnored() {
        val s = store()
        s.save("media/1", 0L, 120_000L)
        s.save("media/2", -5L, 120_000L)
        assertEquals(0L, s.load("media/1"))
        assertEquals(0L, s.load("media/2"))
    }

    @Test
    fun blankIdsAreIgnored() {
        val s = store()
        s.save("   ", 12_345L, 120_000L)
        assertEquals(0L, s.load("   "))
    }

    @Test
    fun clearRemovesPosition() {
        val s = store()
        s.save("media/1", 12_345L, 120_000L)
        s.clear("media/1")
        assertEquals(0L, s.load("media/1"))
    }

    @Test
    fun mediaAreIndependent() {
        val s = store()
        s.save("media/1", 12_345L, 120_000L)
        s.save("media/2", 999L, 120_000L)
        s.clear("media/1")
        assertEquals(0L, s.load("media/1"))
        assertEquals(999L, s.load("media/2"))
    }

    @Test
    fun liveStreamsKeepNoResumeBecauseDurationIsUnknown() {
        val s = store()
        s.save("live/1", 12_345L, 0L)
        assertTrue(s.load("live/1") > 0L)
    }

    @Test
    fun clearAllRemovesEveryPosition() {
        val s = store()
        s.save("media/1", 1_000L, 60_000L)
        s.save("media/2", 2_000L, 60_000L)
        s.clearAll()
        assertEquals(0L, s.load("media/1"))
        assertEquals(0L, s.load("media/2"))
    }

    @Test
    fun clearAllLeavesUnrelatedSettingsUntouched() {
        val settings = MapSettings()
        settings.putString("other_key", "keep")
        val s = PlaybackResumeStore(settings)
        s.save("media/1", 1_000L, 60_000L)
        s.clearAll()
        assertEquals("keep", settings.getString("other_key", ""))
        assertEquals(0L, s.load("media/1"))
    }

    @Test
    fun countReflectsStoredPositions() {
        val s = store()
        assertEquals(0, s.count())
        s.save("media/1", 1_000L, 60_000L)
        s.save("media/2", 2_000L, 60_000L)
        assertEquals(2, s.count())
        s.clear("media/1")
        assertEquals(1, s.count())
        s.clearAll()
        assertEquals(0, s.count())
    }
}