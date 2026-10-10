/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import net.newpipe.app.theme.plusColors

/**
 * Rounded search input with an accent cursor and a clear affordance.
 *
 * @param value Current query text.
 * @param onValueChange Emitted on every edit.
 * @param leadingIcon Optional leading icon (e.g. magnifier).
 * @param onSubmit Invoked when the search IME action is triggered.
 * @param onClear Invoked when the clear button is pressed; defaults to clearing the value.
 */
@Composable
fun SearchBarField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    leadingIcon: Painter? = null,
    onSubmit: () -> Unit = {},
    onClear: () -> Unit = { onValueChange("") }
) {
    val colors = plusColors()
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(text = placeholder, color = colors.textMuted) },
        leadingIcon = leadingIcon?.let { painter ->
            { Icon(painter = painter, contentDescription = null, tint = colors.textSecondary) }
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Text(text = "\u2715", color = colors.textSecondary)
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(999.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceSunken,
            unfocusedContainerColor = colors.surfaceSunken,
            disabledContainerColor = colors.surfaceSunken,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = colors.accent,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary
        )
    )
}
