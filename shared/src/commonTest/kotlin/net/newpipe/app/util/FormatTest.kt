/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.util

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {

    @Test
    fun formatDurationHandlesZeroAndNegative() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5))
    }

    @Test
    fun formatDurationUnderAnHour() {
        assertEquals("0:07", formatDuration(7))
        assertEquals("2:03", formatDuration(123))
        assertEquals("59:59", formatDuration(3599))
    }

    @Test
    fun formatDurationWithHours() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:03", formatDuration(3723))
        assertEquals("10:00:00", formatDuration(36000))
    }

    @Test
    fun formatCountBelowThousandIsExact() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
    }

    @Test
    fun formatCountUsesSuffixes() {
        assertEquals("1K", formatCount(1000))
        assertEquals("1.2K", formatCount(1234))
        assertEquals("1M", formatCount(1_000_000))
        assertEquals("3.4B", formatCount(3_400_000_000))
    }

    @Test
    fun formatBytesBelowKilobyte() {
        assertEquals("0 B", formatBytes(0))
        assertEquals("512 B", formatBytes(512))
        assertEquals("999 B", formatBytes(999))
    }

    @Test
    fun formatBytesUsesDecimalUnits() {
        assertEquals("1 KB", formatBytes(1000))
        assertEquals("1.5 KB", formatBytes(1500))
        assertEquals("29.6 MB", formatBytes(29_600_000))
        assertEquals("1.2 GB", formatBytes(1_200_000_000))
    }

    @Test
    fun formatBytesIgnoresNegative() {
        assertEquals("0 B", formatBytes(-100))
    }
}
