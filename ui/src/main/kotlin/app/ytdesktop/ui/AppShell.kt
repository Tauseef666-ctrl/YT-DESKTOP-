/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Breakpoints driving how many panes are visible at once. */
enum class WindowWidthClass {
    /** Phone-sized / very narrow window: one pane at a time, bottom navigation. */
    Compact,

    /** Tablet / half-screen: navigation rail + content, player below. */
    Medium,

    /** Desktop: navigation + content + player side by side. */
    Expanded,
    ;

    companion object {
        val NavigationRailBreakpoint: Dp = 700.dp
        val ThreePaneBreakpoint: Dp = 1180.dp

        fun fromWidth(width: Dp): WindowWidthClass = when {
            width < NavigationRailBreakpoint -> Compact
            width < ThreePaneBreakpoint -> Medium
            else -> Expanded
        }
    }
}

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF5252),
    secondary = Color(0xFF7C4DFF),
    background = Color(0xFF0E0E10),
    surface = Color(0xFF17171A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFD32F2F),
    secondary = Color(0xFF5E35B1),
)

@Composable
fun YtDesktopTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}

/**
 * Adaptive shell: navigation, browse/search content, and the player.
 *
 * Each slot is a caller-provided composable so that the same layout drives the
 * desktop window now and the Android activity later.
 */
@Composable
fun AppShell(
    widthClass: WindowWidthClass,
    navigation: @Composable () -> Unit,
    content: @Composable () -> Unit,
    player: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (widthClass) {
            WindowWidthClass.Expanded -> ThreePane(navigation, content, player)
            WindowWidthClass.Medium -> TwoPane(navigation, content, player)
            WindowWidthClass.Compact -> SinglePane(navigation, content, player)
        }
    }
}

@Composable
private fun ThreePane(
    navigation: @Composable () -> Unit,
    content: @Composable () -> Unit,
    player: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxSize()) {
        Box(Modifier.width(220.dp).fillMaxHeight()) { navigation() }
        VerticalDivider()
        Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        VerticalDivider()
        Box(Modifier.width(460.dp).fillMaxHeight()) { player() }
    }
}

@Composable
private fun TwoPane(
    navigation: @Composable () -> Unit,
    content: @Composable () -> Unit,
    player: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.weight(1f).fillMaxWidth()) {
            Box(Modifier.width(88.dp).fillMaxHeight()) { navigation() }
            VerticalDivider()
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
        VerticalDivider()
        Box(Modifier.fillMaxWidth().weight(0.7f)) { player() }
    }
}

@Composable
private fun SinglePane(
    navigation: @Composable () -> Unit,
    content: @Composable () -> Unit,
    player: @Composable () -> Unit,
) {
    // For now the browse surface is always the visible pane; the player becomes a
    // full-screen destination once playback starts (wired up in Phase 1).
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        Box(Modifier.fillMaxWidth().height(0.dp)) { player() }
        NavigationBar {
            NavigationBarItem(
                selected = true,
                onClick = {},
                icon = { Text("▶") },
                label = { Text("Browse") },
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Text("↓") },
                label = { Text("Downloads") },
            )
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Text("≡") },
                label = { Text("Library") },
            )
        }
    }
}

/** Simple placeholder used until the browse surfaces land in Phase 1. */
@Composable
fun PlaceholderPane(title: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall)
            Text(
                "Coming in Phase 1",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun PlaybackPane(modifier: Modifier = Modifier) {
    PlaceholderPane("Player", modifier)
}