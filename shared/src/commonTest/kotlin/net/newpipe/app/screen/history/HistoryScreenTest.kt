/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.history

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import net.newpipe.app.extensions.withKoin
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.history_empty_message
import newpipe.shared.generated.resources.history_screen_title
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class HistoryScreenTest {

    @Test
    fun rendersTitleAndEmptyState() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { HistoryScreenContent() },
            onContent = {
                onAllNodesWithText(getString(Res.string.history_screen_title))
                    .onFirst()
                    .assertIsDisplayed()
                onNodeWithText(getString(Res.string.history_empty_message))
                    .assertIsDisplayed()
            }
        )
    }
}