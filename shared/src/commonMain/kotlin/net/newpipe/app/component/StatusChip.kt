/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.plusColors
import net.newpipe.app.theme.spaceSmall

/** Semantic intent of a [StatusChip]. */
enum class StatusKind { INFO, SUCCESS, WARNING, DANGER, LIVE }

/**
 * Small uppercase pill used for download/stream status. Status is also conveyed by the label
 * text, not colour alone.
 */
@Composable
fun StatusChip(text: String, kind: StatusKind, modifier: Modifier = Modifier) {
    val colors = plusColors()
    val (background, foreground) = when (kind) {
        StatusKind.INFO -> colors.surfaceElevated to colors.textSecondary
        StatusKind.SUCCESS -> colors.success.copy(alpha = 0.18f) to colors.success
        StatusKind.WARNING -> colors.warning.copy(alpha = 0.18f) to colors.warning
        StatusKind.DANGER -> colors.danger.copy(alpha = 0.18f) to colors.danger
        StatusKind.LIVE -> colors.accent to Color.White
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = foreground,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = spaceSmall, vertical = 2.dp)
    )
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun StatusChipPreview() {
    StatusChip(text = "DOWNLOADING", kind = StatusKind.INFO)
}
