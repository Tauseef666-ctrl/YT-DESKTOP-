/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import net.newpipe.app.extensions.withKoin
import net.newpipe.app.player.PlayerAction
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.settings_category_shortcuts_title
import newpipe.shared.generated.resources.settings_shortcuts_none_found
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class ShortcutReferenceSectionTest {

    @Test
    fun rendersShortcutCategoryTitle() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { ShortcutReferenceSection() },
            onContent = {
                onNodeWithText(getString(Res.string.settings_category_shortcuts_title))
                    .assertIsDisplayed()
            }
        )
    }

    @Test
    fun searchFiltersRowsByActionLabel() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { ShortcutReferenceSection() },
            onContent = {
                onNode(hasSetTextAction()).performClick().performTextInput("mute")

                onAllNodesWithText(PlayerAction.MUTE.label, substring = true)
                    .assertCountEquals(2)
                onAllNodesWithText(PlayerAction.PLAY_PAUSE.label, substring = true)
                    .assertCountEquals(0)
            }
        )
    }

    @Test
    fun searchWithoutMatchShowsEmptyMessage() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { ShortcutReferenceSection() },
            onContent = {
                onNode(hasSetTextAction()).performClick().performTextInput("zzz")

                onNodeWithText(getString(Res.string.settings_shortcuts_none_found))
                    .assertIsDisplayed()
            }
        )
    }
}
