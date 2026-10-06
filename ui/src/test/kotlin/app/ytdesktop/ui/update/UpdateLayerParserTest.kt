/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The update layer's release parser must not depend on the network to be
 * tested. These fixtures mirror the real GitHub releases/latest payload,
 * including the non-alphabetical order of asset fields (name after size).
 */
class UpdateLayerParserTest {

    private val sampleRelease = """
        {
          "url": "https://api.github.com/repos/Tauseef666-ctrl/YT-DESKTOP-/releases/1",
          "tag_name": "0.2.0",
          "name": "YT Desktop 0.2.0",
          "published_at": "2026-10-08T12:00:00Z",
          "body": "First windnw release\n\r\n- added the update layer\n- fixed playback\n",
          "assets": [
            {
              "url": "https://api.github.com/repos/Tauseef666-ctrl/YT-DESKTOP-/releases/assets/1",
              "id": 1,
              "name": "yt-desktop-0.2.0.msi",
              "label": "windows installer",
              "browser_download_url": "https://github.com/Tauseef666-ctrl/YT-DESKTOP-/releases/download/0.2.0/yt-desktop-0.2.0.msi",
              "size": 12345678,
              "download_count": 4
            },
            {
              "url": "https://api.github.com/repos/Tauseef666-ctrl/YT-DESKTOP-/releases/assets/2",
              "id": 2,
              "name": "yt-desktop-0.2.0-tv.apk",
              "label": "android tv",
              "browser_download_url": "https://github.com/Tauseef666-ctrl/YT-DESKTOP-/releases/download/0.2.0/yt-desktop-0.2.0-tv.apk",
              "size": 23456789,
              "download_count": 1
            },
            {
              "url": "https://api.github.com/repos/Tauseef666-ctrl/YT-DESKTOP-/releases/assets/3",
              "id": 3,
              "name": "yt-desktop-0.2.0.apk",
              "label": "android",
              "browser_download_url": "https://github.com/Tauseef666-ctrl/YT-DESKTOP-/releases/download/0.2.0/yt-desktop-0.2.0.apk",
              "size": 34567890,
              "download_count": 9
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `parses a release, announcements and version`() {
        val release = parseRelease(sampleRelease)
        assertNotNull(release)
        assertEquals("0.2.0", release.version)
        assertEquals("YT Desktop 0.2.0", release.title)
        assertTrue(release.body.contains("update layer"))
        assertTrue(release.body.contains("playback"))
    }

    @Test
    fun `extracts all assets regardless of field order`() {
        val release = parseRelease(sampleRelease)!!
        assertEquals(3, release.assets.size)
        assertEquals(12345678L, release.assets[0].sizeBytes)
        assertTrue(release.assets[0].downloadUrl.endsWith(".msi"))
    }

    @Test
    fun `assets match their target platform`() {
        val release = parseRelease(sampleRelease)!!
        val windows = release.assets.filter { it.matches("Windows") }
        val tv = release.assets.filter { it.matches("Android TV") }
        val phone = release.assets.filter { it.matches("Android") }

        assertEquals(1, windows.size, "windows should see only the .msi")
        assertTrue(windows[0].name.endsWith(".msi"))
        assertEquals(1, tv.size, "TV should see only the -tv apk")
        assertEquals(1, phone.size, "phone should see the plain apk")
        assertTrue(phone[0].name.endsWith(".apk"))
        assertTrue(!phone[0].name.contains("tv", ignoreCase = true))
    }

    @Test
    fun `a version tag with v prefix normalizes`() {
        val release = parseRelease(sampleRelease.replace("\"0.2.0\"", "\"v0.2.0\""))!!
        assertEquals("0.2.0", release.version)
        assertTrue(release.isNewerVersionThan("0.1.0"))
    }
}