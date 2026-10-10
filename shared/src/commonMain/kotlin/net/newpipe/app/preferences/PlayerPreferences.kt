/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.preferences

/**
 * Additive player preferences for the shared player. The resume on/off flag intentionally reuses
 * the existing Android key `enable_playback_resume` so the same setting governs both apps.
 */
object PlayerPreferences {
    const val KEY_RESUME_PLAYBACK = "enable_playback_resume"
    const val DEFAULT_RESUME_PLAYBACK = true

    /** Prefix for stored per-media resume positions (new additive keys). */
    const val PREFIX_RESUME_POSITION = "npp_resume_pos_"
    const val PREFIX_RESUME_DURATION = "npp_resume_dur_"

    /** Items shorter than this are clips; resuming a clip is pointless. */
    const val SHORT_CLIP_MAX_MS = 60_000L
}