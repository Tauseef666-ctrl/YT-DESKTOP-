/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import net.newpipe.app.component.EmptyState
import net.newpipe.app.component.PlusScaffold
import net.newpipe.app.component.SearchBarField
import net.newpipe.app.component.SectionHeader
import net.newpipe.app.component.TopToolbar
import net.newpipe.app.navigation.Navigator
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.spaceLarge
import net.newpipe.app.theme.spaceNormal
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.search_empty_message
import newpipe.shared.generated.resources.search_empty_title
import newpipe.shared.generated.resources.search_screen_placeholder
import newpipe.shared.generated.resources.search_screen_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Search destination. The input is live, but results stay empty until provider wiring lands.
 */
@Composable
fun SearchScreen(navigator: Navigator = koinInject()) {
    var query by remember { mutableStateOf("") }
    SearchScreenContent(
        query = query,
        onQueryChange = { query = it },
        onNavigateUp = { navigator.navigateUp() }
    )
}

@Composable
fun SearchScreenContent(
    query: String = "",
    onQueryChange: (String) -> Unit = {},
    onNavigateUp: () -> Unit = {}
) {
    PlusScaffold(
        toolbar = {
            TopToolbar(
                title = stringResource(Res.string.search_screen_title),
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
            SearchBarField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.padding(horizontal = spaceLarge),
                placeholder = stringResource(Res.string.search_screen_placeholder)
            )
            SectionHeader(title = "Results")
            EmptyState(
                title = stringResource(Res.string.search_empty_title),
                message = stringResource(Res.string.search_empty_message)
            )
            Spacer(Modifier.height(spaceNormal))
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun SearchScreenPreview() {
    SearchScreenContent()
}