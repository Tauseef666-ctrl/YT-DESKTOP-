/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import net.newpipe.app.component.EmptyState
import net.newpipe.app.component.NavEntry
import net.newpipe.app.component.NavRail
import net.newpipe.app.component.PlusScaffold
import net.newpipe.app.component.SectionHeader
import net.newpipe.app.component.TopToolbar
import net.newpipe.app.navigation.Destination
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.plusColors
import net.newpipe.app.theme.spaceLarge
import net.newpipe.app.theme.spaceNormal
import net.newpipe.app.theme.spaceSmall
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.ic_cloud_download
import newpipe.shared.generated.resources.ic_file_download
import newpipe.shared.generated.resources.ic_foreground
import newpipe.shared.generated.resources.ic_headset
import newpipe.shared.generated.resources.ic_palette
import newpipe.shared.generated.resources.nav_history
import newpipe.shared.generated.resources.nav_search
import newpipe.shared.generated.resources.nav_trending
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Desktop home shell: navigation rail + toolbar + content. Only real destinations are wired;
 * unimplemented sections are intentionally absent rather than shown as dead controls.
 */
@Composable
fun HomeScreen(navigator: Navigator = koinInject()) {
    HomeScreenContent(
        onNavigate = { destination ->
            if (destination != Destination.Home) navigator.navigateTo(destination)
        }
    )
}

@Composable
fun HomeScreenContent(
    selectedDestination: Destination = Destination.Home,
    onNavigate: (Destination) -> Unit = {}
) {
    val entries = listOf(
        NavEntry(
            label = "Home",
            selected = selectedDestination == Destination.Home,
            onClick = { onNavigate(Destination.Home) }
        ),
        NavEntry(
            label = stringResource(Res.string.nav_search),
            selected = selectedDestination == Destination.Search,
            onClick = { onNavigate(Destination.Search) }
        ),
        NavEntry(
            label = stringResource(Res.string.nav_trending),
            selected = selectedDestination == Destination.Trending,
            onClick = { onNavigate(Destination.Trending) }
        ),
        NavEntry(
            label = stringResource(Res.string.nav_history),
            selected = selectedDestination == Destination.History,
            onClick = { onNavigate(Destination.History) }
        ),
        NavEntry(
            label = "Player",
            selected = selectedDestination == Destination.Player,
            onClick = { onNavigate(Destination.Player) },
            icon = painterResource(Res.drawable.ic_headset)
        ),
        NavEntry(
            label = "Library",
            selected = selectedDestination == Destination.Library,
            onClick = { onNavigate(Destination.Library) },
            icon = painterResource(Res.drawable.ic_file_download)
        ),
        NavEntry(
            label = "Downloads",
            selected = selectedDestination == Destination.Downloads,
            onClick = { onNavigate(Destination.Downloads) },
            icon = painterResource(Res.drawable.ic_cloud_download)
        ),
        NavEntry(
            label = "Settings",
            selected = selectedDestination == Destination.Settings,
            onClick = { onNavigate(Destination.Settings) },
            icon = painterResource(Res.drawable.ic_palette)
        ),
        NavEntry(
            label = "About",
            selected = selectedDestination == Destination.About,
            onClick = { onNavigate(Destination.About) },
            icon = painterResource(Res.drawable.ic_foreground)
        )
    )

    PlusScaffold(
        rail = { NavRail(entries = entries, header = { RailHeader() }) },
        toolbar = { TopToolbar(title = "Home") }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            WelcomeHero()
            SectionHeader(title = "Continue watching")
            EmptyState(
                title = "Nothing in progress",
                message = "Playback history will appear here once the media engine is wired up."
            )
            SectionHeader(title = "Trending")
            EmptyState(
                title = "No content yet",
                message = "Provider results will appear here as the desktop port lands."
            )
            Spacer(Modifier.height(spaceLarge))
        }
    }
}

@Composable
private fun RailHeader() {
    val colors = plusColors()
    Text(
        text = "NewPipe+",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = colors.textPrimary,
        modifier = Modifier.padding(horizontal = spaceNormal, vertical = spaceNormal)
    )
}

@Composable
private fun WelcomeHero() {
    val colors = plusColors()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(spaceLarge)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(spaceLarge)
    ) {
        Column {
            Text(
                text = "Private, open, and yours",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(Modifier.height(spaceSmall))
            Text(
                text = "NewPipe+ enhances NewPipe with a polished desktop experience — no accounts, no tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    HomeScreenContent()
}
