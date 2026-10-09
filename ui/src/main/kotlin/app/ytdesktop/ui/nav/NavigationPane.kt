/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Top-level destinations. Lives next to AppShell so Android/TV reuse it. */
enum class AppScreen(val label: String) {
    Browse("Browse"),
    Search("Search"),
    Updates("Updates"),
    ;

    val glyph: String
        get() = when (this) {
            Browse -> "▦"
            Search -> "⌕"
            Updates -> "▲"
        }
}

/** The navigation slot content for [app.ytdesktop.ui.AppShell]. */
@Composable
fun NavigationPane(
    screen: AppScreen,
    onSelect: (AppScreen) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(vertical = 8.dp)) {
            // App header (NewPipe drawer style).
            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                Text(
                    "YT Desktop",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "NewPipe-style YouTube client",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AppScreen.entries.forEach { entry ->
                val selected = entry == screen
                val tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(entry) }
                        .background(
                            if (selected) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surface,
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (selected) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .align(Alignment.CenterVertically)
                                .background(MaterialTheme.colorScheme.primary),
                        )
                    }
                    Text(
                        entry.glyph,
                        style = MaterialTheme.typography.titleMedium,
                        color = tint,
                    )
                    Text(
                        entry.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tint,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}