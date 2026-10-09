/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Plan: local library — scanning a chosen folder for playable downloaded media. */
class LocalLibraryTest {

    @Test
    fun `media files are found and paired with a same-name subtitle`() = withTempDir { dir ->
        val video = dir.resolve("My Video.mp4")
        Files.write(video, byteArrayOf(1))
        Files.write(dir.resolve("My Video.srt"), byteArrayOf(2))
        Files.write(dir.resolve("clip.WEBM"), byteArrayOf(3))
        Files.write(dir.resolve("notes.txt"), byteArrayOf(4))

        val found = LocalLibrary.scan(dir)

        assertEquals(listOf("clip", "My Video"), found.map { it.title })
        val withSub = found.first { it.title == "My Video" }
        assertEquals(video, withSub.file)
        assertEquals(dir.resolve("My Video.srt"), withSub.subtitle)
        assertNull(found.first { it.title == "clip" }.subtitle)
    }

    @Test
    fun `non-recursive scan ignores nested folders`() = withTempDir { dir ->
        Files.write(dir.resolve("top.mp4"), byteArrayOf(1))
        val nested = Files.createDirectories(dir.resolve("sub"))
        Files.write(nested.resolve("deep.mkv"), byteArrayOf(2))

        assertEquals(listOf("top"), LocalLibrary.scan(dir, recursive = false).map { it.title })
        assertEquals(listOf("deep", "top"), LocalLibrary.scan(dir, recursive = true).map { it.title })
    }

    @Test
    fun `audio sidecar and vtt subtitles are matched`() = withTempDir { dir ->
        Files.write(dir.resolve("talk.opus"), byteArrayOf(1))
        Files.write(dir.resolve("talk.vtt"), byteArrayOf(2))

        val media = LocalLibrary.scan(dir).single()
        assertEquals("talk", media.title)
        assertEquals(dir.resolve("talk.vtt"), media.subtitle)
        assertTrue(media.sizeBytes > 0)
    }

    @Test
    fun `missing folder yields an empty list`() = withTempDir { dir ->
        assertTrue(LocalLibrary.scan(dir.resolve("nope")).isEmpty())
    }

    private fun withTempDir(block: (Path) -> Unit) {
        val dir = Files.createTempDirectory("ytds-library-test")
        try {
            block(dir)
        } finally {
            dir.toFile().deleteRecursively()
        }
    }
}