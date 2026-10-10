/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.newpipe.app.theme.plusColors

/**
 * Themed dialog consistent with the NewPipe+ surface tokens.
 *
 * @param destructive Tints the confirm action with the danger colour (delete, reset, restore).
 * @param content Optional custom body below [text]; only one of the two is usually needed.
 */
@Composable
fun PlusDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    text: String? = null,
    confirmLabel: String? = null,
    onConfirm: (() -> Unit)? = null,
    dismissLabel: String = "Cancel",
    destructive: Boolean = false,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = plusColors()
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(text = title) },
        text = {
            Column {
                if (text != null) {
                    Text(text = text)
                }
                if (content != null) {
                    content()
                }
            }
        },
        confirmButton = {
            if (confirmLabel != null && onConfirm != null) {
                val color = if (destructive) colors.danger else colors.accent
                TextButton(onClick = onConfirm) { Text(text = confirmLabel, color = color) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = dismissLabel) }
        },
        containerColor = colors.surfaceElevated,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textSecondary
    )
}
