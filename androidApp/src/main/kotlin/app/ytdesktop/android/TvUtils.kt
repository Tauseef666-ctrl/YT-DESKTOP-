/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.android

import android.content.Context
import android.content.pm.PackageManager
import android.view.KeyEvent

/**
 * Runtime detection and key handling for Android TV / Fire TV.
 *
 * Derived from NewPipe's `AndroidTvUtils` (TeamNewPipe/NewPipe PR #2806,
 * "Android TV support"), which ships in NewPipe under GPL-3.0-or-later.
 * The TV *packaging* lives in the `tv` product flavour; behaviour that has to
 * work on any launcher is decided here so a single APK also runs on a TV box.
 */
object TvUtils {

    private const val AMAZON_FEATURE_FIRE_TV = "amazon.hardware.fire_tv"

    /** True when the device is an Android TV or Amazon Fire TV. */
    fun isTv(context: Context): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(AMAZON_FEATURE_FIRE_TV) ||
            pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    /**
     * Keys that mean "activate the focused item" on a D-pad or keyboard.
     * NewPipe needs this because a remote sends DPAD_CENTER while a connected
     * keyboard sends ENTER or SPACE.
     */
    fun isConfirmKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER,
        KeyEvent.KEYCODE_SPACE,
        -> true
        else -> false
    }

    /** Directional keys a D-pad or arrow keys produce. */
    fun isDirectionKey(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        -> true
        else -> false
    }
}
