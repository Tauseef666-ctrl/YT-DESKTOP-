/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import net.newpipe.app.component.EmptyState
import net.newpipe.app.component.PlusScaffold
import net.newpipe.app.component.SectionHeader
import net.newpipe.app.component.TopToolbar
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.spaceNormal
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.history_empty_message
import newpipe.shared.generated.resources.history_empty_title
import newpipe.shared.generated.resources.history_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * History destination. Placeholder until playback history recording is wired up.
 */
@Composable
fun HistoryScreen(navigator: Navigator = koinInject()) {
    HistoryScreenContent(onNavigateUp = { navigator.navigateUp() })
}

@Composable
fun HistoryScreenContent(
    onNavigateUp: () -> Unit = {}
) {
    PlusScaffold(
        toolbar = {
            TopToolbar(
                title = stringResource(Res.string.history_screen_title),
                onBack = onNavigateUp
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = spaceNormal)
        ) {
            SectionHeader(title = "History")
            EmptyState(
                title = stringResource(Res.string.history_empty_title),
                message = stringResource(Res.string.history_empty_message)
            )
            Spacer(Modifier.height(spaceNormal))
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun HistoryScreenPreview() {
    HistoryScreenContent()
}