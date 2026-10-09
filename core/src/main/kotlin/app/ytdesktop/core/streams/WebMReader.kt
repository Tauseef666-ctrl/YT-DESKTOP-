/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Ported from NewPipe's `WebMReader` (org.schabi.newpipe.streams.WebMReader,
 * GPL-3.0-or-later, author kapodamy). Pure-JVM EBML/Matroska segment parser:
 * reads the EBML header, Info/TimecodeScale, Track entries and the SimpleBlock
 * payloads of a (possibly segmented) WebM stream one block at a time.
 */
package app.ytdesktop.core.streams

import app.ytdesktop.core.streams.io.SeekableStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.NoSuchElementException

class WebMReader(source: SeekableStream) {

    private val stream = DataReader(source)

    private var segment: Segment? = null
    private var tracks: Array<WebMTrack>? = null
    private var selectedTrack = 0
    private var done = false
    private var firstSegment = false

    fun parse() {
        var elem = readElement(ID_EMBL)
        if (!readEbml(elem, 1, 2)) {
            throw UnsupportedOperationException("Unsupported EBML data (WebM)")
        }
        ensure(elem)

        elem = untilElement(null, ID_SEGMENT) ?: throw IOException("Fragment element not found")
        segment = readSegment(elem, 0, true)
        tracks = segment!!.tracks
        selectedTrack = -1
        done = false
        firstSegment = true
    }

    fun getAvailableTracks(): Array<WebMTrack> = tracks!!

    fun selectTrack(index: Int): WebMTrack {
        selectedTrack = index
        return tracks!![index]
    }

    fun getNextSegment(): Segment? {
        if (done) return null
        if (firstSegment && segment != null) {
            firstSegment = false
            return segment
        }
        ensure(segment!!.ref)
        val elem = untilElement(null, ID_SEGMENT)
        if (elem == null) {
            done = true
            return null
        }
        segment = readSegment(elem, 0, false)
        return segment
    }

    private fun readNumber(parent: Element): Long {
        var length = parent.contentSize.toInt()
        var value = 0L
        while (length-- > 0) {
            val read = stream.read()
            if (read == -1) throw EOFException()
            value = (value shl 8) or read.toLong()
        }
        return value
    }

    private fun readString(parent: Element): String = String(readBlob(parent), StandardCharsets.UTF_8)

    private fun readBlob(parent: Element): ByteArray {
        val length = parent.contentSize
        val buffer = ByteArray(length.toInt())
        val read = stream.read(buffer)
        if (read < length) throw EOFException()
        return buffer
    }

    private fun readEncodedNumber(): Long {
        val value = stream.read()
        if (value > 0) {
            var size = 1
            var current = value
            var mask = 0x80
            while (size < 9) {
                if ((current and mask) == mask) {
                    mask = 0xFF
                    mask = mask shr size
                    var number = (current and mask).toLong()
                    for (i in 1 until size) {
                        current = stream.read()
                        number = number shl 8
                        number = number or current.toLong()
                    }
                    return number
                }
                mask = mask shr 1
                size++
            }
        }
        throw IOException("Invalid encoded length")
    }

    private fun readElement(): Element {
        val elem = Element()
        elem.offset = stream.position()
        elem.type = readEncodedNumber().toInt()
        elem.contentSize = readEncodedNumber()
        elem.size = elem.contentSize + stream.position() - elem.offset
        return elem
    }

    private fun readElement(expected: Int): Element {
        val elem = readElement()
        if (expected != 0 && elem.type != expected) {
            throw NoSuchElementException(
                "expected ${elementID(expected.toLong())} found ${elementID(elem.type.toLong())}",
            )
        }
        return elem
    }

    private fun untilElement(ref: Element?, vararg expected: Int): Element? {
        while (if (ref == null) stream.available() else stream.position() < (ref.offset + ref.size)) {
            val elem = readElement()
            if (expected.isEmpty()) return elem
            for (type in expected) {
                if (elem.type == type) return elem
            }
            ensure(elem)
        }
        return null
    }

    private fun elementID(type: Long): String = "0x" + java.lang.Long.toHexString(type)

    private fun ensure(ref: Element) {
        val skip = (ref.offset + ref.size) - stream.position()
        if (skip == 0L) {
            return
        } else if (skip < 0) {
            throw EOFException(
                "parser go beyond limits of the Element. type=${elementID(ref.type.toLong())} " +
                    "offset=${ref.offset} size=${ref.size} position=${stream.position()}",
            )
        }
        stream.skipBytes(skip)
    }

    private fun readEbml(ref: Element, minReadVersion: Int, minDocTypeVersion: Int): Boolean {
        var elem = untilElement(ref, ID_EMBL_READ_VERSION) ?: return false
        if (readNumber(elem) > minReadVersion) return false

        elem = untilElement(ref, ID_EMBL_DOC_TYPE) ?: return false
        if (readString(elem) != "webm") return false

        elem = untilElement(ref, ID_EMBL_DOC_TYPE_READ_VERSION) ?: return false
        return readNumber(elem) <= minDocTypeVersion
    }

    private fun readInfo(ref: Element): Info {
        val info = Info()
        var elem = untilElement(ref, ID_TIMECODE_SCALE, ID_DURATION)
        while (elem != null) {
            when (elem.type) {
                ID_TIMECODE_SCALE -> info.timecodeScale = readNumber(elem)
                ID_DURATION -> info.duration = readNumber(elem)
            }
            ensure(elem)
            elem = untilElement(ref, ID_TIMECODE_SCALE, ID_DURATION)
        }
        if (info.timecodeScale == 0L) throw NoSuchElementException("Element Timecode not found")
        return info
    }

    private fun readSegment(ref: Element, trackLacingExpected: Int, metadataExpected: Boolean): Segment {
        val obj = Segment(ref)
        var elem = untilElement(ref, ID_INFO, ID_TRACKS, ID_CLUSTER)
        while (elem != null) {
            if (elem.type == ID_CLUSTER) {
                obj.currentCluster = elem
                break
            }
            when (elem.type) {
                ID_INFO -> obj.info = readInfo(elem)
                ID_TRACKS -> obj.tracks = readTracks(elem, trackLacingExpected)
            }
            ensure(elem)
            elem = untilElement(ref, ID_INFO, ID_TRACKS, ID_CLUSTER)
        }

        if (metadataExpected && (obj.info == null || obj.tracks.isEmpty())) {
            throw RuntimeException(
                "Cluster element found without Info and/or Tracks element at position ${ref.offset}",
            )
        }
        return obj
    }

    private fun readTracks(ref: Element, lacingExpected: Int): Array<WebMTrack> {
        val trackEntries = ArrayList<WebMTrack>(2)
        var elemTrackEntry = untilElement(ref, ID_TRACK_ENTRY)
        while (elemTrackEntry != null) {
            val entry = WebMTrack()
            var drop = false
            var elem = untilElement(elemTrackEntry)
            while (elem != null) {
                when (elem.type) {
                    ID_TRACK_NUMBER -> entry.trackNumber = readNumber(elem)
                    ID_TRACK_TYPE -> entry.trackType = readNumber(elem).toInt()
                    ID_CODEC_ID -> entry.codecId = readString(elem)
                    ID_CODEC_PRIVATE -> entry.codecPrivate = readBlob(elem)
                    ID_AUDIO, ID_VIDEO -> entry.bMetadata = readBlob(elem)
                    ID_DEFAULT_DURATION -> entry.defaultDuration = readNumber(elem)
                    ID_FLAG_LACING -> drop = readNumber(elem).toInt() != lacingExpected
                    ID_CODEC_DELAY -> entry.codecDelay = readNumber(elem)
                    ID_SEEK_PRE_ROLL -> entry.seekPreRoll = readNumber(elem)
                }
                ensure(elem)
                elem = untilElement(elemTrackEntry)
            }
            if (!drop) trackEntries.add(entry)
            ensure(elemTrackEntry)
            elemTrackEntry = untilElement(ref, ID_TRACK_ENTRY)
        }

        val entries = trackEntries.toTypedArray()
        for (entry in entries) {
            entry.kind = when (entry.trackType) {
                1 -> TrackKind.Video
                2 -> TrackKind.Audio
                else -> TrackKind.Other
            }
        }
        return entries
    }

    private fun readSimpleBlock(ref: Element): SimpleBlock {
        val obj = SimpleBlock(ref)
        obj.trackNumber = readEncodedNumber()
        obj.relativeTimeCode = stream.readShort()
        obj.flags = stream.read().toByte()
        obj.dataSize = ((ref.offset + ref.size) - stream.position()).toInt()
        obj.createdFromBlock = ref.type == ID_BLOCK

        // NOTE: lacing is not implemented; it would be mixed with the stream data
        if (obj.dataSize < 0) {
            throw IOException("Unexpected SimpleBlock element size, missing ${-obj.dataSize} bytes")
        }
        return obj
    }

    private fun readCluster(ref: Element): Cluster {
        val obj = Cluster(ref)
        val elem = untilElement(ref, ID_TIMECODE)
            ?: throw NoSuchElementException("Cluster at ${ref.offset} without Timecode element")
        obj.timecode = readNumber(elem)
        return obj
    }

    class Element {
        var type = 0
        var offset = 0L
        var contentSize = 0L
        var size = 0L
    }

    class Info {
        var timecodeScale = 0L
        var duration = 0L
    }

    open class WebMTrack {
        var trackNumber = 0L
        var trackType = 0
        var codecId: String = ""
        var codecPrivate: ByteArray? = null
        var bMetadata: ByteArray? = null
        var kind: TrackKind = TrackKind.Other
        var defaultDuration = -1L
        var codecDelay = -1L
        var seekPreRoll = -1L
    }

    inner class Segment internal constructor(val ref: Element) {
        var info: Info? = null
        var tracks: Array<WebMTrack> = emptyArray()
        var currentCluster: Element? = null
        var firstClusterInSegment = true

        fun getNextCluster(): Cluster? {
            if (done) return null
            val cluster = currentCluster
            if (firstClusterInSegment && cluster != null) {
                firstClusterInSegment = false
                return readCluster(cluster)
            }
            ensure(cluster!!)
            val elem = untilElement(ref, ID_CLUSTER) ?: return null
            currentCluster = elem
            return readCluster(elem)
        }
    }

    class SimpleBlock internal constructor(val ref: Element) {
        var data: InputStream? = null
        var createdFromBlock = false
        var trackNumber = 0L
        var relativeTimeCode: Short = 0
        var absoluteTimeCodeNs = 0L
        var flags: Byte = 0
        var dataSize = 0

        fun isKeyframe(): Boolean = (flags.toInt() and 0x80) == 0x80
    }

    inner class Cluster internal constructor(val ref: Element) {
        var currentSimpleBlock: SimpleBlock? = null
        var currentBlockGroup: Element? = null
        var timecode = 0L

        private fun insideClusterBounds(): Boolean = stream.position() >= (ref.offset + ref.size)

        fun getNextSimpleBlock(): SimpleBlock? {
            if (insideClusterBounds()) return null

            val group = currentBlockGroup
            if (group != null) {
                ensure(group)
                currentBlockGroup = null
                currentSimpleBlock = null
            } else {
                currentSimpleBlock?.let { ensure(it.ref) }
            }

            while (!insideClusterBounds()) {
                var elem = untilElement(ref, ID_SIMPLE_BLOCK, ID_GROUP_BLOCK) ?: return null
                if (elem.type == ID_GROUP_BLOCK) {
                    currentBlockGroup = elem
                    elem = untilElement(elem, ID_BLOCK)
                    if (elem == null) {
                        ensure(currentBlockGroup!!)
                        currentBlockGroup = null
                        continue
                    }
                }

                val block = readSimpleBlock(elem)
                if (block.trackNumber == tracks!![selectedTrack].trackNumber) {
                    block.data = stream.getView(block.dataSize)
                    block.absoluteTimeCodeNs = (block.relativeTimeCode + timecode) * segment!!.info!!.timecodeScale
                    return block
                }
                ensure(elem)
            }
            return null
        }
    }

    enum class TrackKind { Audio, Video, Other }

    companion object {
        private const val ID_EMBL = 0x0A45DFA3
        private const val ID_EMBL_READ_VERSION = 0x02F7
        private const val ID_EMBL_DOC_TYPE = 0x0282
        private const val ID_EMBL_DOC_TYPE_READ_VERSION = 0x0285

        private const val ID_SEGMENT = 0x08538067

        private const val ID_INFO = 0x0549A966
        private const val ID_TIMECODE_SCALE = 0x0AD7B1
        private const val ID_DURATION = 0x489

        private const val ID_TRACKS = 0x0654AE6B
        private const val ID_TRACK_ENTRY = 0x2E
        private const val ID_TRACK_NUMBER = 0x57
        private const val ID_TRACK_TYPE = 0x03
        private const val ID_CODEC_ID = 0x06
        private const val ID_CODEC_PRIVATE = 0x23A2
        private const val ID_VIDEO = 0x60
        private const val ID_AUDIO = 0x61
        private const val ID_DEFAULT_DURATION = 0x3E383
        private const val ID_FLAG_LACING = 0x1C
        private const val ID_CODEC_DELAY = 0x16AA
        private const val ID_SEEK_PRE_ROLL = 0x16BB

        private const val ID_CLUSTER = 0x0F43B675
        private const val ID_TIMECODE = 0x67
        private const val ID_SIMPLE_BLOCK = 0x23
        private const val ID_BLOCK = 0x21
        private const val ID_GROUP_BLOCK = 0x20
    }
}