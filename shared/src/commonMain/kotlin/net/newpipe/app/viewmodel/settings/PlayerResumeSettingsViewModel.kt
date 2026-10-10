/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.viewmodel.settings

import androidx.lifecycle.ViewModel
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import net.newpipe.app.player.PlaybackResumeStore
import net.newpipe.app.preferences.PlayerPreferences
import org.koin.core.annotation.KoinViewModel

/**
 * Drives the F9 "playback resume" settings section: the on/off toggle (reusing the same
 * `enable_playback_resume` key as Android) and the privacy action that forgets every stored
 * position. Reads live from [Settings] so a change applies immediately, without a restart.
 */
@KoinViewModel
class PlayerResumeSettingsViewModel(
    private val settings: Settings,
    private val resumeStore: PlaybackResumeStore
) : ViewModel() {

    val resumeEnabled: StateFlow<Boolean>
        field = MutableStateFlow(
            settings.getBoolean(
                PlayerPreferences.KEY_RESUME_PLAYBACK,
                PlayerPreferences.DEFAULT_RESUME_PLAYBACK
            )
        )

    private val _savedPositions = MutableStateFlow(resumeStore.count())
    val savedPositions: StateFlow<Int> = _savedPositions

    fun setResumeEnabled(enabled: Boolean) {
        settings.putBoolean(PlayerPreferences.KEY_RESUME_PLAYBACK, enabled)
        resumeEnabled.value = enabled
    }

    fun clearResumePositions() {
        resumeStore.clearAll()
        _savedPositions.value = resumeStore.count()
    }

    fun refreshSavedPositions() {
        _savedPositions.value = resumeStore.count()
    }
}