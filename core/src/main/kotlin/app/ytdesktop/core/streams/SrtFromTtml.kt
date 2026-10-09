/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.streams

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Node

/**
 * YouTube TTML caption payload -> SubRip (.srt). Self-contained and offline, so
 * it is unit-tested without a network (Phase 2.3 muxer set).
 *
 * The tasks in YouTube's TTML come out of `subtitles` timedtext as
 * `<body><div><p begin end>…</p>…` with `<br/>` for line breaks and optional
 * spans for styling. Each caption becomes one SRT block:
 *
 *     1
 *     00:00:00,000 --> 00:00:03,000
 *     text
 *
 * Anything the DOM cannot decode (malformed XML, unknown caption entities,
 * absent begin times) degrades to `null` rather than throwing.
 */
object SrtFromTtmlWriter {

    fun convert(ttmlXml: String): String? {
        val doc = try {
            val f = DocumentBuilderFactory.newInstance()
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setQuietly(f, "http://apache.org/xml/features/disallow-doctype-decl", true)
            setQuietly(f, "http://xml.org/sax/features/external-general-entities", false)
            setQuietly(f, "http://xml.org/sax/features/external-parameter-entities", false)
            f.newDocumentBuilder()
                .parse(ByteArrayInputStream(ttmlXml.toByteArray(StandardCharsets.UTF_8)))
        } catch (e: Exception) {
            return null
        }

        // Qualified-name matching, not getElementsByTagNameNS: JDK DOM
        // factories are not guaranteed namespace-aware, and caption <p> is
        // never prefixed, so the qualified name "p" is what we can rely on
        // whichever mode the parser picked.
        val captionNodes = doc.getElementsByTagName("p")
        val blocks = mutableListOf<String>()
        for (i in 0 until captionNodes.length) {
            val p = captionNodes.item(i)
            val begin = p.attributes?.getNamedItem("begin")?.nodeValue ?: continue
            val beginMs = parseSrtTime(begin) ?: continue
            val endMs = p.attributes?.getNamedItem("end")?.nodeValue
                ?.let(::parseSrtTime)
                ?: (beginMs + FALLBACK_END_MS)
            val text = captionText(p).trim().replace('\u00A0', ' ')
            if (text.isBlank()) continue
            blocks += "${blocks.size + 1}\n${formatClock(beginMs)} --> ${formatClock(endMs)}\n$text"
        }
        if (blocks.isEmpty()) return null
        return blocks.joinToString("\n\n") + "\n"
    }

    /** Concatenates text; `<br/>` becomes a newline, nested spans flatten out. */
    private fun captionText(node: Node): String = buildString {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            when (child.nodeType) {
                Node.TEXT_NODE -> append(child.nodeValue)
                Node.ELEMENT_NODE ->
                    if (nameOf(child) == "br") append('\n') else append(captionText(child))
            }
        }
    }

    /** Qualified name that works whether or not the DOM is namespace-aware. */
    private fun nameOf(node: Node): String = node.localName ?: node.nodeName

    /** Accepts `HH:MM:SS.mmm` (or `HH:MM:SS`) and bare-seconds `"7.326"`. */
    private fun parseSrtTime(value: String): Long? =
        convertStringToMs(value)
            ?.takeIf { it >= 0L }

    private fun convertStringToMs(value: String): Long? {
        return when {
            ':' in value -> {
                val parts = value.split(':', limit = 3)
                if (parts.size != 3) return null
                val h = parts[0].toLongOrNull() ?: return null
                val m = parts[1].toLongOrNull() ?: return null
                val s = parts[2].toDoubleOrNull() ?: return null
                h * 3_600_000L + m * 60_000L + (s * 1000.0).toLong()
            }
            else -> (value.toDoubleOrNull() ?: return null).let { (it * 1000.0).toLong() }
        }
    }

    private fun formatClock(ms: Long): String {
        val totalSeconds = ms / 1000
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        val millis = ms % 1000
        return "%02d:%02d:%02d,%03d".format(h, m, s, millis)
    }

    private fun setQuietly(f: DocumentBuilderFactory, feature: String, value: Boolean) {
        try {
            f.setFeature(feature, value)
        } catch (e: Exception) {
            // Some JVM XML implementations reject rare features; the secure
            // processing flag above still applies.
        }
    }

    private const val FALLBACK_END_MS = 2000L
}