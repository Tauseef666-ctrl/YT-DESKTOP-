/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.newpipe.app.extensions.withKoin
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.clear_resume_positions_title
import newpipe.shared.generated.resources.enable_playback_resume_title
import newpipe.shared.generated.resources.resume_positions_cleared
import newpipe.shared.generated.resources.settings_category_resume_title
import org.jetbrains.compose.resources.getString

@OptIn(ExperimentalTestApi::class)
class PlayerResumeSettingsSectionTest {

    @Test
    fun rendersResumeToggleAndClearRow() = runComposeUiTest {
        withKoin(
            modules = emptyList(),
            content = { PlayerResumeSettingsSectionContent() },
            onContent = {
                onNodeWithText(getString(Res.string.settings_category_resume_title))
                    .assertIsDisplayed()
                onNodeWithText(getString(Res.string.enable_playback_resume_title))
                    .assertIsDisplayed()
                onNodeWithText(getString(Res.string.clear_resume_positions_title))
                    .assertIsDisplayed()
            }
        )
    }

    @Test
    fun clearRowInvokesCallbackAndShowsConfirmation() = runComposeUiTest {
        var cleared = false
        withKoin(
            modules = emptyList(),
            content = {
                PlayerResumeSettingsSectionContent(
                    savedPositions = 3,
                    onClearResumePositions = { cleared = true }
                )
            },
            onContent = {
                onNodeWithText(getString(Res.string.clear_resume_positions_title))
                    .performClick()
                assertTrue(cleared)
                onNodeWithText(getString(Res.string.resume_positions_cleared))
                    .assertIsDisplayed()
            }
        )
    }

    @Test
    fun toggleForwardsNewValue() = runComposeUiTest {
        var forwarded: Boolean? = null
        withKoin(
            modules = emptyList(),
            content = {
                PlayerResumeSettingsSectionContent(
                    resumeEnabled = true,
                    onResumeEnabledChange = { forwarded = it }
                )
            },
            onContent = {
                onNodeWithText(getString(Res.string.enable_playback_resume_title))
                    .performClick()
                assertEquals(false, forwarded)
            }
        )
    }
}