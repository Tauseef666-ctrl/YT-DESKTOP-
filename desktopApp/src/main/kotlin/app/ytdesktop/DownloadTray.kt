/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import app.ytdesktop.core.download.DownloadJob
import app.ytdesktop.core.download.DownloadManager
import java.awt.Color
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.RenderingHints
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import kotlinx.coroutines.flow.collectLatest

/**
 * Plan 2.4 tray progress: while any download is active, a system-tray icon
 * shows a live tooltip ("2 downloads · 34%") and a menu shortcut back to the
 * downloads screen. The icon appears when work starts and is removed when the
 * queue drains. Everything is guarded so headless/CI sessions are unaffected.
 */
@Composable
fun DownloadTray(
    manager: DownloadManager,
    onOpenDownloads: () -> Unit,
) {
    LaunchedEffect(manager) {
        manager.jobs.collectLatest { jobs -> updateTray(jobs, onOpenDownloads) }
    }
    DisposableEffect(Unit) {
        onDispose { removeTray() }
    }
}

private var trayIcon: TrayIcon? = null

private fun updateTray(jobs: List<DownloadJob>, onOpenDownloads: () -> Unit) {
    if (!SystemTray.isSupported()) return
    try {
        val activeJobs = jobs.filter { it.active }
        if (activeJobs.isEmpty()) {
            removeTray()
            return
        }

        val total = activeJobs.mapNotNull { it.bytesTotal }.takeIf { it.size == activeJobs.size }?.sum()
        val done = activeJobs.sumOf { it.bytesDone }
        val percent = if (total != null && total > 0) done * 100 / total else 0

        val tray = SystemTray.getSystemTray()
        val icon = trayIcon ?: TrayIcon(trayImage(), "YT Desktop").also {
            it.isImageAutoSize = true
            it.popupMenu = PopupMenu().apply {
                add(MenuItem("Open downloads").apply { addActionListener { onOpenDownloads() } })
            }
            tray.add(it)
            trayIcon = it
        }
        icon.toolTip = "YT Desktop — ${activeJobs.size} download(s) · $percent%"
    } catch (e: Exception) {
        println("DownloadTray: ${e.message}")
    }
}

private fun removeTray() {
    val icon = trayIcon ?: return
    trayIcon = null
    runCatching {
        if (SystemTray.isSupported()) SystemTray.getSystemTray().remove(icon)
    }
}

/** A small NewPipe-red icon with a white down arrow, drawn at runtime. */
private fun trayImage(): BufferedImage {
    val size = 16
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color(0xFF, 0x52, 0x52)
    g.fillOval(0, 0, size, size)
    g.color = Color.WHITE
    g.fillRect(size / 2 - 1, 3, 2, 6)
    g.fillPolygon(intArrayOf(size / 2 - 4, size / 2 + 4, size / 2), intArrayOf(7, 7, 12), 3)
    g.dispose()
    return image
}