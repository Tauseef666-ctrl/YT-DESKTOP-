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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.ytdesktop.core.download.DownloadManager
import app.ytdesktop.core.download.DownloadPart
import app.ytdesktop.core.download.DownloadRequest
import app.ytdesktop.core.errors.YtException
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.YoutubeService
import app.ytdesktop.player.PlayQueueController
import app.ytdesktop.player.PlayerChrome
import app.ytdesktop.player.VlcPlayerEngine
import app.ytdesktop.player.VlcVideoSurface
import app.ytdesktop.player.rememberVlcPlayer
import app.ytdesktop.storage.SearchHistoryStore
import app.ytdesktop.ui.AppShell
import app.ytdesktop.ui.WindowWidthClass
import app.ytdesktop.ui.YtDesktopTheme
import app.ytdesktop.ui.browse.ChannelScreen
import app.ytdesktop.ui.browse.TrendingScreen
import app.ytdesktop.ui.download.DownloadsScreen
import app.ytdesktop.ui.nav.AppScreen
import app.ytdesktop.ui.nav.NavigationPane
import app.ytdesktop.ui.search.SearchScreen
import app.ytdesktop.ui.update.AppInfo
import app.ytdesktop.ui.update.UpdatePane
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.launch

private val INITIAL_WIDTH: Dp = 1280.dp

// Mirrors the Gradle `yt.version` property (default "0.1.2").
private const val APP_VERSION = "0.1.2"

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
        val scope = rememberCoroutineScope()
        val controller = remember(service, engine) { PlayQueueController(scope, service, engine) }
        var screen by remember { mutableStateOf(AppScreen.Browse) }
        var openedChannel by remember { mutableStateOf<String?>(null) }
        val onVideoClick: (StreamItem) -> Unit = controller::play
        val onChannelClick: (String) -> Unit = { openedChannel = it }
        val historyStore = remember { SearchHistoryStore.inDefaultDir() }
        var searchHistory by remember { mutableStateOf(historyStore.load()) }
        val downloadsDir = remember {
            Path.of(System.getProperty("user.home"), ".yt-desktop", "downloads")
                .also { runCatching { Files.createDirectories(it) } }
        }
        val downloads = remember { DownloadManager() }
        var downloadNotice by remember { mutableStateOf<String?>(null) }
        val onDownload: (StreamItem) -> Unit = { item ->
            scope.launch {
                runCatching { service.resolveDownload(item.url) }
                    .onSuccess { resolved ->
                        val base = safeFileName(resolved.title)
                        val parts = buildList {
                            resolved.video?.let { video ->
                                add(
                                    DownloadPart(
                                        label = "video",
                                        request = DownloadRequest(
                                            url = video.url,
                                            destination = downloadsDir.resolve("$base [video].${video.extension}"),
                                        ),
                                    ),
                                )
                            }
                            add(
                                DownloadPart(
                                    label = "audio",
                                    request = DownloadRequest(
                                        url = resolved.audio.url,
                                        destination = downloadsDir.resolve("$base [audio].${resolved.audio.extension}"),
                                    ),
                                ),
                            )
                        }
                        downloads.enqueue(resolved.title, parts)
                        downloadNotice = null
                        screen = AppScreen.Downloads
                        openedChannel = null
                    }
                    .onFailure { e ->
                        downloadNotice = (e as? YtException)?.message
                            ?: e.message
                            ?: "Could not start download"
                        screen = AppScreen.Downloads
                        openedChannel = null
                    }
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
                    NavigationPane(
                        screen = screen,
                        onSelect = { selected ->
                            screen = selected
                            openedChannel = null
                        },
                    )
                },
                content = {
                    val channelUrl = openedChannel
                    if (channelUrl != null) {
                        ChannelScreen(
                            service = service,
                            channelUrl = channelUrl,
                            onBack = { openedChannel = null },
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onDownload = onDownload,
                        )
                    } else when (screen) {
                        AppScreen.Browse -> TrendingScreen(
                            service = service,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                            onDownload = onDownload,
                        )
                        AppScreen.Search -> SearchScreen(
                            service = service,
                            onVideoClick = onVideoClick,
                            history = searchHistory,
                            onQuerySubmitted = { historyStore.append(it); searchHistory = historyStore.load() },
                            onClearHistory = { historyStore.clear(); searchHistory = emptyList() },
                            onChannelClick = onChannelClick,
                            onDownload = onDownload,
                        )
                        AppScreen.Downloads -> DownloadsScreen(
                            manager = downloads,
                            notice = downloadNotice,
                            onOpenFolder = ::openFolder,
                        )
                        AppScreen.Updates -> UpdatePane(
                            info = AppInfo(currentVersion = APP_VERSION, platformLabel = "Windows"),
                            openUrl = ::openBrowser,
                        )
                    }
                },
player = {
                    Column(Modifier.fillMaxSize()) {
                        VlcVideoSurface(vlcPlayer, Modifier.weight(1f).fillMaxWidth())
                        PlayerChrome(
                            engine = engine,
                            modifier = Modifier.fillMaxWidth(),
                            queue = controller.items,
                            currentIndex = controller.currentIndex,
                            resolving = controller.resolving,
                            onSelectQueue = controller::playAt,
                            onNext = controller::next,
                            onPrevious = controller::previous,
                        )
                    }
                },
            )
        }
        DownloadTray(
            manager = downloads,
            onOpenDownloads = {
                screen = AppScreen.Downloads
                openedChannel = null
            },
        )
    }
}

private fun openFolder(folder: Path) {
    runCatching { Desktop.getDesktop().open(folder.toFile()) }
        .onFailure { println("openFolder: $folder -> ${it.message}") }
}

private fun safeFileName(title: String): String {
    val cleaned = title.map { ch ->
        if (ch.isLetterOrDigit() || ch in " -_().&,'![]") ch else '_'
    }.joinToString("").trim().trimEnd('.').take(120)
    return cleaned.ifBlank { "video" }
}

private fun openBrowser(url: String) {
    runCatching { Desktop.getDesktop().browse(URI(url)) }
        .onFailure { println("openBrowser: $url -> ${it.message}") }
}