/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import kotlinx.coroutines.runBlocking
import net.newpipe.app.player.LocalMediaFile
import net.newpipe.app.player.diffByPath
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JVMLocalMediaScannerTest {

    private val scanner = JVMLocalMediaScanner()
    private val tempDirs = mutableListOf<java.io.File>()

    @AfterTest
    fun cleanup() {
        tempDirs.forEach { it.deleteRecursively() }
    }

    private fun tempDir(): java.io.File =
        Files.createTempDirectory("newpipe-scan").toFile().also { tempDirs += it }

    @Test
    fun scansSupportedFilesRecursivelyAndSortsByName() = runBlocking {
        val dir = tempDir()
        dir.resolve("b.mp3").writeText("x")
        dir.resolve("a.mp4").writeText("x")
        dir.resolve("notes.txt").writeText("x")
        dir.resolve("ignored.pdf").writeText("x")
        dir.resolve("Movie.MKV").writeText("x")
        val sub = dir.resolve("sub").apply { mkdirs() }
        sub.resolve("clip.webm").writeText("x")

        val files = scanner.scanFolder(dir.absolutePath)

        assertEquals(listOf("a.mp4", "b.mp3", "clip.webm", "Movie.MKV"), files.map { it.name })
        assertTrue(files.all { it.url.startsWith("file:") }, "every entry must be a playable file URL")
        assertTrue(files.all { it.durationMs == null }, "duration must not be guessed")
    }

    @Test
    fun classifiesAudioAndVideo() = runBlocking {
        val dir = tempDir()
        dir.resolve("song.flac").writeText("x")
        dir.resolve("movie.mp4").writeText("x")

        val files = scanner.scanFolder(dir.absolutePath).associateBy { it.name }

        assertTrue(files.getValue("song.flac").audioOnly)
        assertEquals("audio/flac", files.getValue("song.flac").mimeType)
        assertFalse(files.getValue("movie.mp4").audioOnly)
        assertEquals("video/mp4", files.getValue("movie.mp4").mimeType)
    }

    @Test
    fun reportsRealSize() = runBlocking {
        val dir = tempDir()
        val content = "0123456789"
        dir.resolve("a.mp4").writeText(content)

        val file = scanner.scanFolder(dir.absolutePath).single()

        assertEquals(content.toByteArray().size.toLong(), file.sizeBytes)
    }

    @Test
    fun emptyFolderYieldsNoItems() = runBlocking {
        val dir = tempDir()
        assertTrue(scanner.scanFolder(dir.absolutePath).isEmpty())
    }

    @Test
    fun missingFolderFails() {
        val missing = tempDir().resolve("does-not-exist")
        assertFailsWith<IllegalArgumentException> {
            runBlocking { scanner.scanFolder(missing.absolutePath) }
        }
    }

    @Test
    fun filePathIsNotAFolder() {
        val file = tempDir().resolve("a.mp4").apply { writeText("x") }
        assertFailsWith<IllegalArgumentException> {
            runBlocking { scanner.scanFolder(file.absolutePath) }
        }
    }

    @Test
    fun incrementalScanReportsAddedFile() = runBlocking {
        val dir = tempDir()
        dir.resolve("new.mp4").writeText("hello")

        val diff = scanner.incrementalScan(dir, emptyList())

        assertEquals(listOf("new.mp4"), diff.added.map { it.name })
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.changed.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun incrementalScanReportsRemovedFile() = runBlocking {
        val dir = tempDir()
        val file = dir.resolve("gone.mp4").apply { writeText("x") }
        val absolutePath = file.absolutePath
        val previous = scanner.scanFolder(dir.absolutePath)
        file.delete()

        val diff = scanner.incrementalScan(dir, previous)

        assertEquals(listOf(absolutePath), diff.removed)
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.changed.isEmpty())
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun incrementalScanDetectsModifiedFile() = runBlocking {
        val dir = tempDir()
        val file = dir.resolve("edit.mp4").apply {
            writeText("old")
            setLastModified(1_000_000_000_000L)
        }
        val previous = scanner.scanFolder(dir.absolutePath)

        val newContent = "a much longer replacement body"
        file.writeText(newContent)
        file.setLastModified(2_000_000_000_000L)

        val diff = scanner.incrementalScan(dir, previous)

        assertTrue(diff.added.isEmpty())
        assertEquals(listOf("edit.mp4"), diff.changed.map { it.name })
        assertEquals(newContent.toByteArray().size.toLong(), diff.changed.single().sizeBytes)
        assertTrue(diff.unchanged.isEmpty())
    }

    @Test
    fun incrementalScanKeepsUnchangedFiles() = runBlocking {
        val dir = tempDir()
        dir.resolve("stable.mp3").apply {
            writeText("x")
            setLastModified(1_000_000_000_000L)
        }
        val previous = scanner.scanFolder(dir.absolutePath)

        val diff = scanner.incrementalScan(dir, previous)

        assertEquals(listOf("stable.mp3"), diff.unchanged.map { it.name })
        assertTrue(diff.added.isEmpty())
        assertTrue(diff.removed.isEmpty())
        assertTrue(diff.changed.isEmpty())
    }

    @Test
    fun diffByPathClassifiesNewRemovedChangedAndUnchanged() {
        val pathA = "/media/a.mp4"
        val pathB = "/media/b.mp4"
        val pathC = "/media/c.mp4"
        val pathD = "/media/d.mp4"

        val a = media(pathA, size = 10L, modified = 100L)
        val b = media(pathB, size = 20L, modified = 200L)
        val bChanged = media(pathB, size = 25L, modified = 200L)
        val c = media(pathC, size = 30L, modified = 300L)

        val diff = diffByPath(listOf(a, b, media(pathD, 40L, 400L)), listOf(a, bChanged, c))

        assertEquals(listOf(pathC), diff.added.map { it.path })
        assertEquals(listOf(pathD), diff.removed)
        assertEquals(listOf(pathB), diff.changed.map { it.path })
        assertEquals(listOf(pathA), diff.unchanged.map { it.path })
    }

    private fun media(path: String, size: Long, modified: Long): LocalMediaFile = LocalMediaFile(
        id = path,
        path = path,
        name = path.substringAfterLast('/'),
        extension = path.substringAfterLast('.'),
        mimeType = "video/mp4",
        url = "file://$path",
        sizeBytes = size,
        modifiedAt = modified
    )
}
