/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import net.newpipe.app.extensions.withKoin
import net.newpipe.app.player.LocalMediaFile
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.library_view_grid
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class LibrarySearchTest {

    private fun file(name: String, audioOnly: Boolean = false): LocalMediaFile = LocalMediaFile(
        id = "/media/$name",
        path = "/media/$name",
        name = name,
        extension = name.substringAfterLast('.', ""),
        mimeType = if (audioOnly) "audio/x" else "video/x",
        url = "file:///media/$name",
        audioOnly = audioOnly
    )

    @Test
    fun searchFiltersRowsByName() = runComposeUiTest {
        val items = listOf(
            file("Mission Impossible.mkv"),
            file("Tutorial.mp4")
        )
        withKoin(
            modules = emptyList(),
            content = {
                var filters by remember { mutableStateOf(LibraryFilters()) }
                LibraryScreenContent(
                    items = items,
                    filters = filters,
                    onFiltersChange = { filters = it }
                )
            },
            onContent = {
                onNodeWithTag(TEST_TAG_LIBRARY_SEARCH).performClick().performTextInput("mission")

                onAllNodesWithText("Mission Impossible.mkv").assertCountEquals(1)
                onAllNodesWithText("Tutorial.mp4").assertCountEquals(0)
            }
        )
    }

    @Test
    fun viewModeToggleShowsGridTiles() = runComposeUiTest {
        val items = listOf(
            file("Alpha.mp4"),
            file("Bravo.mp4"),
            file("Charlie.mp4"),
            file("Delta.mp4")
        )
        withKoin(
            modules = emptyList(),
            content = {
                var filters by remember { mutableStateOf(LibraryFilters()) }
                LibraryScreenContent(
                    items = items,
                    filters = filters,
                    onFiltersChange = { filters = it }
                )
            },
            onContent = {
                onNodeWithText(getString(Res.string.library_view_grid)).performClick()

                items.forEach { item ->
                    onAllNodesWithText(item.name).assertCountEquals(1)
                }
            }
        )
    }
}
