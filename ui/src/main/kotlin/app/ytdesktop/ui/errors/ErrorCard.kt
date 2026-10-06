/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.errors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.errors.ErrorInfo
import app.ytdesktop.core.errors.UserAction

/**
 * The shared error surface (plan.md 1.8): renders a core [ErrorInfo] as its
 * user action, its stable message, and an optional retry.
 */
@Composable
fun ErrorCard(
    info: ErrorInfo,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                userActionLabel(info.userAction),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                info.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            if (onRetry != null) {
                Button(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) { Text("Retry") }
            }
        }
    }
}

internal fun userActionLabel(action: UserAction): String = when (action) {
    UserAction.SEARCH -> "Search failed"
    UserAction.SUGGESTIONS -> "Suggestions unavailable"
    UserAction.TRENDING -> "Couldn't load trending"
    UserAction.CHANNEL -> "Couldn't load channel"
    UserAction.CHANNEL_TAB -> "Couldn't load this tab"
    UserAction.STREAM_RESOLUTION -> "Couldn't prepare playback"
    UserAction.VIDEO_PAGE -> "Couldn't load the video page"
    UserAction.PLAYBACK -> "Playback failed"
    UserAction.UNKNOWN -> "Something went wrong"
}