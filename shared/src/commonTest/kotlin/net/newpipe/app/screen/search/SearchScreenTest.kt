/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import net.newpipe.app.extensions.withKoin
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.search_empty_message
import newpipe.shared.generated.resources.search_screen_placeholder
import newpipe.shared.generated.resources.search_screen_title
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class SearchScreenTest {

    @Test
    fun rendersTitleAndSearchBox() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { SearchScreenContent() },
            onContent = {
                onNodeWithText(getString(Res.string.search_screen_title))
                    .assertIsDisplayed()
                onNodeWithText(getString(Res.string.search_screen_placeholder))
                    .assertIsDisplayed()
            }
        )
    }

    @Test
    fun typingUpdatesQuery() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = {
                var query by remember { mutableStateOf("") }
                SearchScreenContent(query = query, onQueryChange = { query = it })
            },
            onContent = {
                onNode(hasSetTextAction()).performClick().performTextInput("abc")

                onNodeWithText("abc").assertExists()
            }
        )
    }

    @Test
    fun showsHonestEmptyState() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { SearchScreenContent() },
            onContent = {
                onNodeWithText(getString(Res.string.search_empty_message))
                    .assertIsDisplayed()
            }
        )
    }
}