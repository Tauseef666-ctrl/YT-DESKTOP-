/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.player

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayQueueTest {

    private fun item(id: String) = MediaItem(id = id, title = "Track $id")

    private fun sample() = listOf(item("a"), item("b"), item("c"), item("d"))

    @Test
    fun startsAtFirstItem() {
        val queue = PlayQueue(sample())
        assertEquals(listOf("a", "b", "c", "d"), queue.items.map { it.id })
        assertEquals("a", queue.current?.id)
        assertEquals(4, queue.size)
        assertFalse(queue.isEmpty)
    }

    @Test
    fun nextAdvancesInOrder() {
        val queue = PlayQueue(sample())
        assertEquals("b", queue.next()?.id)
        assertEquals("c", queue.next()?.id)
        assertEquals("c", queue.current?.id)
    }

    @Test
    fun nextReturnsNullAtEndWhenRepeatOff() {
        val queue = PlayQueue(listOf(item("a"), item("b")))
        queue.next()
        assertNull(queue.next())
        assertFalse(queue.hasNext())
    }

    @Test
    fun nextWrapsWhenRepeatAll() {
        val queue = PlayQueue(listOf(item("a"), item("b")), repeatMode = RepeatMode.ALL)
        queue.next()
        assertEquals("a", queue.next()?.id)
        assertTrue(queue.hasNext())
    }

    @Test
    fun nextRepeatsCurrentWhenRepeatOne() {
        val queue = PlayQueue(sample(), repeatMode = RepeatMode.ONE)
        assertEquals("a", queue.next()?.id)
        assertEquals("a", queue.current?.id)
    }

    @Test
    fun previousStepsBackAndWrapsOnlyWithRepeatAll() {
        val off = PlayQueue(sample())
        assertNull(off.previous())

        val all = PlayQueue(sample(), repeatMode = RepeatMode.ALL)
        assertEquals("d", all.previous()?.id)
    }

    @Test
    fun enqueueAppendsAndEnqueueNextInserts() {
        val queue = PlayQueue(listOf(item("a"), item("b")))
        queue.enqueueNext(item("x"))
        queue.enqueue(item("z"))
        assertEquals(listOf("a", "x", "b", "z"), queue.items.map { it.id })
    }

    @Test
    fun removeAtKeepsCurrentByIdentityAndClamps() {
        val queue = PlayQueue(sample())
        queue.jumpTo(2) // c
        val removed = queue.removeAt(0) // remove a (before current)
        assertEquals("a", removed?.id)
        assertEquals("c", queue.current?.id)
        assertEquals(listOf("b", "c", "d"), queue.items.map { it.id })

        queue.removeAt(queue.size - 1) // remove d (after current)
        assertEquals("c", queue.current?.id)
    }

    @Test
    fun removingLastCurrentClampsIndex() {
        val queue = PlayQueue(listOf(item("a"), item("b")))
        queue.jumpTo(1)
        queue.removeAt(1)
        assertEquals("a", queue.current?.id)
    }

    @Test
    fun moveReordersAndKeepsCurrent() {
        val queue = PlayQueue(sample())
        queue.jumpTo(2) // c
        assertTrue(queue.move(0, 3)) // move a to end
        assertEquals(listOf("b", "c", "d", "a"), queue.items.map { it.id })
        assertEquals("c", queue.current?.id)
    }

    @Test
    fun toggleShuffleKeepsCurrentAndRestoresOrder() {
        val queue = PlayQueue(sample(), random = Random(1234))
        queue.jumpTo(2) // c
        val original = queue.items.map { it.id }

        queue.toggleShuffle()
        assertTrue(queue.isShuffled)
        assertEquals("c", queue.current?.id)
        assertEquals(original.toSet(), queue.items.map { it.id }.toSet())

        queue.toggleShuffle()
        assertFalse(queue.isShuffled)
        assertEquals(original, queue.items.map { it.id })
        assertEquals("c", queue.current?.id)
    }

    @Test
    fun clearResetsToEmpty() {
        val queue = PlayQueue(sample())
        queue.clear()
        assertTrue(queue.isEmpty)
        assertNull(queue.current)
        assertNull(queue.next())
    }
}
