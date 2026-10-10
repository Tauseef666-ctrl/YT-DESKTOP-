/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JVMPopOutCoordinatorTest {

    @Test
    fun reportsSupportOnJvm() {
        assertTrue(JVMPopOutCoordinator().supported)
    }

    @Test
    fun toggleFlipsTheBridgeState() {
        DesktopMiniPlayerBridge.isOpen.value = false
        val coordinator = JVMPopOutCoordinator()

        coordinator.toggle()
        assertTrue(DesktopMiniPlayerBridge.isOpen.value)

        coordinator.toggle()
        assertFalse(DesktopMiniPlayerBridge.isOpen.value)
    }

    @Test
    fun isOpenMirrorsTheBridgeState() {
        DesktopMiniPlayerBridge.isOpen.value = true
        val coordinator = JVMPopOutCoordinator()
        assertTrue(coordinator.isOpen.value)

        DesktopMiniPlayerBridge.isOpen.value = false
        assertFalse(coordinator.isOpen.value)
    }
}