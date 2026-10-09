/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.streams

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Plan 2.3 (muxer set), first slice: YouTube's `subtitles` timedtext TTML -> .srt.
 * Fully offline — nothing here touches the network, so it also runs in every
 * CI box unconditionally.
 */
class SrtFromTtmlTest {

    @Test
    fun `typical youtube ttml becomes indexed srt with br and entities`() {
        val ttml = """
            <?xml version="1.0" encoding="utf-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml" xmlns:s="http://www.w3.org/ns/ttml#styling">
              <body>
                <div>
                  <p begin="00:00:01.000" end="00:00:04.500">Hello &amp; welcome</p>
                  <p begin="00:00:04.500" end="00:00:07.326">line one<br/>line two — ok?</p>
                  <p begin="00:00:07.326" end="00:00:09.000">test it</p>
                </div>
              </body>
            </tt>
        """.trimIndent()

        val srt = SrtFromTtmlWriter.convert(ttml)

        assertEquals(
            """
            1
            00:00:01,000 --> 00:00:04,500
            Hello & welcome

            2
            00:00:04,500 --> 00:00:07,326
            line one
            line two — ok?

            3
            00:00:07,326 --> 00:00:09,000
            test it
            """.trimIndent() + "\n",
            srt,
        )
    }

    @Test
    fun `span styling is flattened and times handle hours`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div>
                <p begin="01:02:03.250" end="01:02:05.000">
                  <span style="s1">bold <span>nested</span></span> tail
                </p>
              </div></body>
            </tt>
        """.trimIndent()

        val srt = SrtFromTtmlWriter.convert(ttml)

        assertEquals("1\n01:02:03,250 --> 01:02:05,000\nbold nested tail\n", srt)
    }

    @Test
    fun `missing end falls back to begin plus two seconds`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div><p begin="00:00:10.000">only begin</p></div></body>
            </tt>
        """.trimIndent()

        assertEquals("1\n00:00:10,000 --> 00:00:12,000\nonly begin\n", SrtFromTtmlWriter.convert(ttml))
    }

    @Test
    fun `bare seconds begin is accepted`() {
        val ttml = """
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body><div><p begin="7.326">decimal</p></div></body>
            </tt>
        """.trimIndent()

        assertEquals("1\n00:00:07,326 --> 00:00:09,326\ndecimal\n", SrtFromTtmlWriter.convert(ttml))
    }

    @Test
    fun `no captions yields null`() {
        val ttml = """<tt xmlns="http://www.w3.org/ns/ttml"><body><div></div></body></tt>"""
        assertNull(SrtFromTtmlWriter.convert(ttml))
    }

    @Test
    fun `malformed xml yields null`() {
        assertNull(SrtFromTtmlWriter.convert("<tt><body><p begin=>broken</tt>"))
    }
}