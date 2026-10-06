/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.YoutubeService
import app.ytdesktop.player.PlayerChrome
import app.ytdesktop.player.VlcPlayerEngine
import app.ytdesktop.player.VlcVideoSurface
import app.ytdesktop.player.rememberVlcPlayer
import app.ytdesktop.ui.AppShell
import app.ytdesktop.ui.WindowWidthClass
import app.ytdesktop.ui.YtDesktopTheme
import app.ytdesktop.ui.browse.TrendingScreen
import app.ytdesktop.ui.nav.AppScreen
import app.ytdesktop.ui.nav.NavigationPane
import app.ytdesktop.ui.search.SearchScreen
import app.ytdesktop.ui.update.AppInfo
import app.ytdesktop.ui.update.UpdatePane
import java.awt.Desktop
import java.net.URI

private val INITIAL_WIDTH: Dp = 1280.dp

// Mirrors the Gradle `yt.version` property (default "0.1.0").
private const val APP_VERSION = "0.1.0"

fun main() {
    // Must happen before VLCJ touches its native library.
    BundledVlc.configure()

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "YT Desktop",
            state = rememberWindowState(width = INITIAL_WIDTH, height = 820.dp),
        ) {
            App()
        }
    }
}

@Composable
fun App() {
    YtDesktopTheme {
        var windowWidth by remember { mutableStateOf(INITIAL_WIDTH) }
        val density = LocalDensity.current
        val widthClass = WindowWidthClass.fromWidth(windowWidth)
        val vlcPlayer = rememberVlcPlayer()
        val engine = remember { VlcPlayerEngine(vlcPlayer) }
        val service = remember { YoutubeService() }
        var screen by remember { mutableStateOf(AppScreen.Browse) }
        var pendingVideo: StreamItem? by remember { mutableStateOf(null) }
        val onVideoClick: (StreamItem) -> Unit = { pendingVideo = it }

        LaunchedEffect(pendingVideo) {
            val item = pendingVideo ?: return@LaunchedEffect
            pendingVideo = null
            val resolved = runCatching { service.resolvePlayback(item.url) }.getOrNull()
            if (resolved != null) {
                engine.play(
                    source = resolved.video,
                    title = resolved.title,
                    companionAudio = resolved.audio,
                )
            } else {
                engine.reportLoadFailure(item.title)
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .onSizeChanged { size ->
                    windowWidth = with(density) { size.width.toDp() }
                },
        ) {
            AppShell(
                widthClass = widthClass,
                navigation = {
                    NavigationPane(screen = screen, onSelect = { screen = it })
                },
                content = {
                    when (screen) {
                        AppScreen.Browse -> TrendingScreen(service = service, onVideoClick = onVideoClick)
                        AppScreen.Search -> SearchScreen(service = service, onVideoClick = onVideoClick)
                        AppScreen.Updates -> UpdatePane(
                            info = AppInfo(currentVersion = APP_VERSION, platformLabel = "Windows"),
                            openUrl = ::openBrowser,
                        )
                    }
                },
                player = {
                    Column(Modifier.fillMaxSize()) {
                        VlcVideoSurface(vlcPlayer, Modifier.weight(1f).fillMaxWidth())
                        PlayerChrome(engine, Modifier.fillMaxWidth())
                    }
                },
            )
        }
    }
}

private fun openBrowser(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
        .onFailure { println("openBrowser: $url -> ${it.message}") }
}