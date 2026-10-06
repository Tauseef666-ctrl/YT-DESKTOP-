/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.android

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Leanback landing surface.
 *
 * Everything reachable with a D-pad: no gesture-only affordances, no hover
 * state, and a focus ring that is visible from across a room — the three
 * problems NewPipe tracks as #3687, #11197 and #13853 for TV users.
 *
 * Navigation itself is handled by Compose's focus system, which moves focus
 * with the arrow/D-pad keys and activates with DPAD_CENTER or ENTER.
 */
@Composable
fun TvShell(modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 56.dp, vertical = 40.dp),
    ) {
        Text(
            "YT Desktop",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Use the arrow keys to move, OK to select",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "Sections",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TvCard("Browse", "Trending, subscriptions and search")
            TvCard("Downloads", "Saved videos and audio")
            TvCard("Library", "History, playlists and bookmarks")
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Android TV build",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TvCard("Leanback launcher", "Banner + D-pad entry point")
            TvCard("No gestures", "Every action has a key equivalent")
            TvCard("Focus first", "Controls are reachable without touch")
        }
    }
}

@Composable
private fun TvCard(title: String, subtitle: String) {
    var focused by remember { mutableStateOf(false) }

    OutlinedCard(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (focused) 3.dp else 1.dp,
            color = if (focused) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (focused) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            },
        ),
        // clickable supplies the focus target, the DPAD_CENTER/ENTER activation
        // and the semantics entry, so no separate focusable() is needed.
        modifier = Modifier
            .width(280.dp)
            .height(if (focused) 168.dp else 160.dp)
            .onFocusChanged { focused = it.isFocused }
            .clickable { /* wired to navigation in Phase 1 */ },
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = if (focused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Small helper kept for the Phase 1 player surface. */
@Composable
fun TvFocusHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.Transparent, style = MaterialTheme.typography.labelSmall)
    }
}
