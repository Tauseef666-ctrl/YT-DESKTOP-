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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import app.ytdesktop.core.library.LocalLibrary
import app.ytdesktop.core.library.LocalMedia
import app.ytdesktop.core.model.PlaybackSource
import app.ytdesktop.core.model.StreamItem
import app.ytdesktop.core.service.YoutubeService
import app.ytdesktop.player.PlayQueueController
import app.ytdesktop.player.PlayerChrome
import app.ytdesktop.player.VlcPlayerEngine
import app.ytdesktop.player.VlcVideoSurface
import app.ytdesktop.player.rememberVlcPlayer
import app.ytdesktop.storage.LibraryFolderStore
import app.ytdesktop.storage.SearchHistoryStore
import app.ytdesktop.ui.AppShell
import app.ytdesktop.ui.WindowWidthClass
import app.ytdesktop.ui.YtDesktopTheme
import app.ytdesktop.ui.browse.ChannelScreen
import app.ytdesktop.ui.browse.TrendingScreen
import app.ytdesktop.ui.library.LibraryScreen
import app.ytdesktop.ui.nav.AppScreen
import app.ytdesktop.ui.nav.NavigationPane
import app.ytdesktop.ui.search.SearchScreen
import app.ytdesktop.ui.update.AppInfo
import app.ytdesktop.ui.update.UpdatePane
import java.awt.Desktop
import java.net.URI
import java.nio.file.Path
import javax.swing.JFileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val INITIAL_WIDTH: Dp = 1280.dp

// Mirrors the Gradle `yt.version` property (default "0.1.0").
private const val APP_VERSION = "0.2.0"

fun main() {
    // Must happen before VLCJ touches its native library.
    BundledVlc.configure()

    application {
        val windowState = rememberWindowState(width = INITIAL_WIDTH, height = 820.dp)
        var fullscreen by remember { mutableStateOf(false) }
        Window(
            onCloseRequest = ::exitApplication,
            title = "YT Desktop",
            state = windowState,
        ) {
            App(
                isFullscreen = fullscreen,
                onToggleFullscreen = {
                    fullscreen = !fullscreen
                    windowState.placement =
                        if (fullscreen) WindowPlacement.Fullscreen else WindowPlacement.Floating
                },
            )
        }
    }
}

@Composable
fun App(
    isFullscreen: Boolean = false,
    onToggleFullscreen: () -> Unit = {},
) {
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

        val folderStore = remember { LibraryFolderStore.inDefaultDir() }
        var libraryFolder by remember { mutableStateOf(folderStore.load()?.let { Path.of(it) }) }
        var libraryMedia by remember { mutableStateOf<List<LocalMedia>>(emptyList()) }
        var libraryScanning by remember { mutableStateOf(false) }
        var libraryRefreshKey by remember { mutableStateOf(0) }

        LaunchedEffect(libraryFolder, libraryRefreshKey) {
            val folder = libraryFolder
            libraryScanning = folder != null
            libraryMedia = if (folder == null) {
                emptyList()
            } else {
                withContext(Dispatchers.IO) { LocalLibrary.scan(folder) }
            }
            libraryScanning = false
        }

        val chooseLibraryFolder: () -> Unit = {
            val chooser = JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = "Choose the folder with your downloaded videos"
                libraryFolder?.let { currentDirectory = it.toFile() }
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                val chosen = chooser.selectedFile.toPath()
                libraryFolder = chosen
                folderStore.save(chosen.toString())
                screen = AppScreen.Library
                openedChannel = null
            }
        }

        val playLocal: (LocalMedia) -> Unit = { media ->
            engine.play(
                source = PlaybackSource.Local(
                    path = media.file.toString(),
                    subtitleFile = media.subtitle?.toString(),
                ),
                title = media.title,
            )
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
                        )
                    } else when (screen) {
                        AppScreen.Browse -> TrendingScreen(
                            service = service,
                            onVideoClick = onVideoClick,
                            onChannelClick = onChannelClick,
                        )
                        AppScreen.Search -> SearchScreen(
                            service = service,
                            onVideoClick = onVideoClick,
                            history = searchHistory,
                            onQuerySubmitted = { historyStore.append(it); searchHistory = historyStore.load() },
                            onClearHistory = { historyStore.clear(); searchHistory = emptyList() },
                            onChannelClick = onChannelClick,
                        )
                        AppScreen.Library -> LibraryScreen(
                            folder = libraryFolder,
                            media = libraryMedia,
                            scanning = libraryScanning,
                            onChooseFolder = chooseLibraryFolder,
                            onRefresh = { libraryRefreshKey++ },
                            onPlay = playLocal,
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
                            isFullscreen = isFullscreen,
                            onToggleFullscreen = onToggleFullscreen,
                        )
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