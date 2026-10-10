/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import net.newpipe.app.composable.PreferenceCategoryTitle
import net.newpipe.app.composable.PreferenceRow
import net.newpipe.app.composable.SwitchPreference
import net.newpipe.app.viewmodel.settings.PlayerResumeSettingsViewModel
import newpipe.shared.generated.resources.Res
import newpipe.shared.generated.resources.clear_resume_positions_summary
import newpipe.shared.generated.resources.clear_resume_positions_title
import newpipe.shared.generated.resources.enable_playback_resume_summary
import newpipe.shared.generated.resources.enable_playback_resume_title
import newpipe.shared.generated.resources.resume_positions_cleared
import newpipe.shared.generated.resources.settings_category_resume_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * F9 "playback resume" settings section: the on/off toggle (networked to the same
 * `enable_playback_resume` key as Android) and a privacy action that forgets every stored
 * resume position. Stateless content is in [PlayerResumeSettingsSectionContent].
 */
@Composable
fun PlayerResumeSettingsSection(viewModel: PlayerResumeSettingsViewModel = koinViewModel()) {
    val resumeEnabled by viewModel.resumeEnabled.collectAsState()
    val savedPositions by viewModel.savedPositions.collectAsState()

    PlayerResumeSettingsSectionContent(
        resumeEnabled = resumeEnabled,
        savedPositions = savedPositions,
        onResumeEnabledChange = viewModel::setResumeEnabled,
        onClearResumePositions = viewModel::clearResumePositions
    )
}

@Composable
fun PlayerResumeSettingsSectionContent(
    resumeEnabled: Boolean = true,
    savedPositions: Int = 0,
    onResumeEnabledChange: (Boolean) -> Unit = {},
    onClearResumePositions: () -> Unit = {}
) {
    var cleared by remember { mutableStateOf(false) }

    Column {
        PreferenceCategoryTitle(stringResource(Res.string.settings_category_resume_title))
        SwitchPreference(
            title = stringResource(Res.string.enable_playback_resume_title),
            summary = stringResource(Res.string.enable_playback_resume_summary),
            checked = resumeEnabled,
            onCheckedChange = {
                cleared = false
                onResumeEnabledChange(it)
            }
        )
        PreferenceRow(
            title = stringResource(Res.string.clear_resume_positions_title),
            summary = if (cleared) {
                stringResource(Res.string.resume_positions_cleared)
            } else {
                stringResource(Res.string.clear_resume_positions_summary, savedPositions)
            },
            onClick = {
                onClearResumePositions()
                cleared = true
            }
        )
    }
}