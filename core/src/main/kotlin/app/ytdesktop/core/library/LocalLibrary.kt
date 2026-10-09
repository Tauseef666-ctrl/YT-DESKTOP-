/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.library

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension

/** One playable file found on disk, with its matching sidecar subtitle if any. */
data class LocalMedia(
    val title: String,
    val file: Path,
    val subtitle: Path?,
    val sizeBytes: Long,
)

/**
 * Scans a user-chosen folder for media the app can play — primarily videos
 * downloaded by NewPipe or this app. Media types are matched by extension, and
 * a sidecar subtitle with the same base name (`.srt`/`.vtt`/…) is paired so the
 * player can load it automatically. Pure JVM, so it is unit-tested offline.
 */
object LocalLibrary {

    val MEDIA_EXTENSIONS: Set<String> = setOf(
        "mp4", "mkv", "webm", "m4a", "opus", "ogg", "ogv", "oga",
        "mp3", "aac", "flac", "wav", "3gp", "mov", "avi", "ts", "m3u8",
    )

    val SUBTITLE_EXTENSIONS: List<String> = listOf("srt", "vtt", "ass", "ssa")

    /** Lists playable media under [folder] (recursively by default), title-sorted. */
    fun scan(folder: Path, recursive: Boolean = true): List<LocalMedia> {
        if (!Files.isDirectory(folder)) return emptyList()
        val depth = if (recursive) Int.MAX_VALUE else 1
        val stream = Files.walk(folder, depth)
        val files: List<Path> = try {
            stream.filter { Files.isRegularFile(it) }.toList()
        } finally {
            stream.close()
        }

        return files
            .filter { it.extension.lowercase() in MEDIA_EXTENSIONS }
            .map { file ->
                LocalMedia(
                    title = file.nameWithoutExtension,
                    file = file,
                    subtitle = sidecarSubtitle(file),
                    sizeBytes = runCatching { Files.size(file) }.getOrDefault(0L),
                )
            }
            .sortedWith(compareBy({ it.title.lowercase() }, { it.file.toString().lowercase() }))
    }

    private fun sidecarSubtitle(file: Path): Path? {
        val base = file.nameWithoutExtension
        return SUBTITLE_EXTENSIONS.asSequence()
            .map { file.resolveSibling("$base.$it") }
            .firstOrNull { Files.isRegularFile(it) }
    }
}