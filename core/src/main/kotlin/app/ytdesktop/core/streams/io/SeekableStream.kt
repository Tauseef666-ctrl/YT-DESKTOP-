/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Ported from NewPipe's `SharpStream` (org.schabi.newpipe.streams.io.SharpStream,
 * GPL-3.0-or-later, author kapodamy), reduced to the seekable-file case YT
 * Desktop needs. The muxers read and write local files, so a single
 * `RandomAccessFile`-backed abstraction covers both input and output.
 */
package app.ytdesktop.core.streams.io

import java.io.Closeable
import java.io.RandomAccessFile
import java.nio.file.Path

/** A bidirectional byte stream that supports the read/seek/write mix a muxer needs. */
interface SeekableStream : Closeable {
    fun read(): Int
    fun read(buffer: ByteArray, offset: Int, count: Int): Int
    fun skip(amount: Long): Long
    fun available(): Long
    fun rewind()
    fun canRewind(): Boolean
    fun canSeek(): Boolean
    fun write(buffer: ByteArray, offset: Int, count: Int)
    fun seek(offset: Long)
    fun position(): Long
}

/** [SeekableStream] over a local file, backed by [RandomAccessFile]. */
class FileSeekableStream private constructor(private val raf: RandomAccessFile) : SeekableStream {

    override fun read(): Int = raf.read()

    override fun read(buffer: ByteArray, offset: Int, count: Int): Int =
        raf.read(buffer, offset, count)

    override fun skip(amount: Long): Long {
        if (amount <= 0) return 0
        val start = raf.filePointer
        val target = (start + amount).coerceAtMost(raf.length())
        raf.seek(target)
        return target - start
    }

    override fun available(): Long = raf.length() - raf.filePointer

    override fun rewind() {
        raf.seek(0)
    }

    override fun canRewind(): Boolean = true

    override fun canSeek(): Boolean = true

    override fun write(buffer: ByteArray, offset: Int, count: Int) {
        raf.write(buffer, offset, count)
    }

    override fun seek(offset: Long) {
        raf.seek(offset)
    }

    override fun position(): Long = raf.filePointer

    override fun close() {
        raf.close()
    }

    companion object {
        /** Opens [path] read-only. */
        fun openRead(path: Path): FileSeekableStream =
            FileSeekableStream(RandomAccessFile(path.toFile(), "r"))

        /** Opens [path] read-write (created if missing), for muxer output. */
        fun openReadWrite(path: Path): FileSeekableStream =
            FileSeekableStream(RandomAccessFile(path.toFile(), "rw"))
    }
}