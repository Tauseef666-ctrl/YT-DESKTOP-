/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Ported from NewPipe's `DataReader` (org.schabi.newpipe.streams.DataReader,
 * GPL-3.0-or-later, author kapodamy): a buffered, byte-counting reader over a
 * [SeekableStream] with big-endian primitive helpers and lazy sub-stream views.
 */
package app.ytdesktop.core.streams

import app.ytdesktop.core.streams.io.SeekableStream
import java.io.EOFException
import java.io.InputStream

internal class DataReader(private val stream: SeekableStream) {

    private var position = 0L
    private var view: InputStream? = null
    private var viewSize = 0

    private val readBuffer = ByteArray(BUFFER_SIZE)
    private var readOffset = readBuffer.size
    private var readCount = 0
    private val primitive = ShortArray(LONG_SIZE)

    fun position(): Long = position

    fun read(): Int {
        if (fillBuffer()) return -1
        position++
        readCount--
        return readBuffer[readOffset++].toInt() and 0xFF
    }

    fun skipBytes(byteAmount: Long): Long {
        var amount = byteAmount
        if (readCount < 0) {
            return 0
        } else if (readCount == 0) {
            amount = stream.skip(amount)
        } else {
            if (readCount > amount) {
                readCount -= amount.toInt()
                readOffset += amount.toInt()
            } else {
                amount = readCount + stream.skip(amount - readCount)
                readCount = 0
                readOffset = readBuffer.size
            }
        }
        position += amount
        return amount
    }

    fun readInt(): Int {
        primitiveRead(INTEGER_SIZE)
        return (primitive[0].toInt() shl 24) or (primitive[1].toInt() shl 16) or
            (primitive[2].toInt() shl 8) or primitive[3].toInt()
    }

    fun readUnsignedInt(): Long = readInt().toLong() and 0xFFFFFFFFL

    fun readShort(): Short {
        primitiveRead(SHORT_SIZE)
        return ((primitive[0].toInt() shl 8) or primitive[1].toInt()).toShort()
    }

    fun readLong(): Long {
        primitiveRead(LONG_SIZE)
        val high = (primitive[0].toLong() shl 24) or (primitive[1].toLong() shl 16) or
            (primitive[2].toLong() shl 8) or primitive[3].toLong()
        val low = (primitive[4].toLong() shl 24) or (primitive[5].toLong() shl 16) or
            (primitive[6].toLong() shl 8) or primitive[7].toLong()
        return (high shl 32) or low
    }

    fun read(buffer: ByteArray): Int = read(buffer, 0, buffer.size)

    fun read(buffer: ByteArray, off: Int, c: Int): Int {
        var offset = off
        var count = c
        if (readCount < 0) return -1
        var total = 0

        if (count >= readBuffer.size) {
            if (readCount > 0) {
                System.arraycopy(readBuffer, readOffset, buffer, offset, readCount)
                readOffset += readCount
                offset += readCount
                count -= readCount
                total = readCount
                readCount = 0
            }
            total += maxOf(stream.read(buffer, offset, count), 0)
        } else {
            while (count > 0 && !fillBuffer()) {
                val read = minOf(readCount, count)
                System.arraycopy(readBuffer, readOffset, buffer, offset, read)
                readOffset += read
                readCount -= read
                offset += read
                count -= read
                total += read
            }
        }

        position += total
        return total
    }

    fun available(): Boolean = readCount > 0 || stream.available() > 0

    fun rewind() {
        stream.rewind()
        if ((position - viewSize) > 0) {
            viewSize = 0
        } else {
            viewSize += position.toInt()
        }
        position = 0
        readOffset = readBuffer.size
        readCount = 0
    }

    fun canRewind(): Boolean = stream.canRewind()

    /**
     * Wraps this reader into an [InputStream] bounded to [size] bytes. Reads
     * through the view advance the underlying reader; [available] reflects the
     * remaining view size.
     */
    fun getView(size: Int): InputStream {
        val streamsView = view ?: object : InputStream() {
            override fun read(): Int {
                if (viewSize < 1) return -1
                val res = this@DataReader.read()
                if (res > 0) viewSize--
                return res
            }

            override fun read(buffer: ByteArray, offset: Int, count: Int): Int {
                if (viewSize < 1) return -1
                val res = this@DataReader.read(buffer, offset, minOf(viewSize, count))
                viewSize -= res
                return res
            }

            override fun skip(amount: Long): Long {
                if (viewSize < 1) return 0
                val res = this@DataReader.skipBytes(minOf(amount, viewSize.toLong())).toInt()
                viewSize -= res
                return res.toLong()
            }

            override fun available(): Int = viewSize

            override fun close() {
                viewSize = 0
            }

            override fun markSupported(): Boolean = false
        }.also { view = it }

        viewSize = size
        return streamsView
    }

    private fun primitiveRead(amount: Int) {
        val buffer = ByteArray(amount)
        val read = read(buffer, 0, amount)
        if (read != amount) {
            throw EOFException("Truncated stream, missing ${amount - read} bytes")
        }
        for (i in 0 until amount) {
            primitive[i] = (buffer[i].toInt() and 0xFF).toShort()
        }
    }

    private fun fillBuffer(): Boolean {
        if (readCount < 0) return true
        if (readOffset >= readBuffer.size) {
            readCount = stream.read(readBuffer, 0, readBuffer.size)
            if (readCount < 1) {
                readCount = -1
                return true
            }
            readOffset = 0
        }
        return readCount < 1
    }

    companion object {
        const val SHORT_SIZE = 2
        const val LONG_SIZE = 8
        const val INTEGER_SIZE = 4
        const val FLOAT_SIZE = 4
        private const val BUFFER_SIZE = 128 * 1024
    }
}