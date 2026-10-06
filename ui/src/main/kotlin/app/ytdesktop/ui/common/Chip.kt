/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** A compact selectable pill used by kiosk pickers and channel tabs. */
@Composable
fun SurfaceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}