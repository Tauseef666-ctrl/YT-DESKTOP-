/*
 * SPDX-FileCopyrightText: 2026 NewPipe e.V. <https://newpipe-ev.de>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package net.newpipe.app.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import net.newpipe.app.preview.ThemePreviewProvider
import net.newpipe.app.theme.plusColors
import net.newpipe.app.theme.spaceNormal
import net.newpipe.app.theme.spaceSmall
import net.newpipe.app.theme.spaceXXSmall

/** Lifecycle state of a download mission. */
enum class DownloadStatus(val label: String, val kind: StatusKind) {
    QUEUED("QUEUED", StatusKind.INFO),
    DOWNLOADING("DOWNLOADING", StatusKind.INFO),
    PAUSED("PAUSED", StatusKind.WARNING),
    COMPLETED("COMPLETED", StatusKind.SUCCESS),
    FAILED("FAILED", StatusKind.DANGER)
}

private val DownloadThumbShape = RoundedCornerShape(8.dp)

/**
 * A download row: thumbnail, title, status chip, progress (when active) and meta line.
 *
 * @param progress Progress in `0f..1f`; pass null for indeterminate/complete rows.
 * @param actions Trailing action slot (pause/resume/cancel/open).
 */
@Composable
fun DownloadItem(
    title: String,
    status: DownloadStatus,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    thumbnail: Painter? = null,
    progress: Float? = null,
    speedText: String? = null,
    sizeText: String? = null,
    onClick: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = plusColors()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = spaceNormal, vertical = spaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(16f / 9f)
                .clip(DownloadThumbShape)
                .background(colors.surfaceElevated)
        ) {
            if (thumbnail != null) {
                Image(
                    painter = thumbnail,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (progress != null && status == DownloadStatus.DOWNLOADING) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    color = colors.accent,
                    trackColor = colors.border,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                )
            }
        }
        Spacer(Modifier.width(spaceNormal))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(spaceXXSmall))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(spaceSmall))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spaceSmall)
            ) {
                StatusChip(text = status.label, kind = status.kind)
                val meta = listOfNotNull(speedText, sizeText).joinToString(" • ")
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spaceXXSmall),
            content = actions
        )
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@PreviewLightDark
@Composable
private fun DownloadItemPreview() {
    DownloadItem(
        title = "NewPipe+ — private and open",
        status = DownloadStatus.DOWNLOADING,
        subtitle = "Video • 1080p",
        progress = 0.42f,
        speedText = "1.2 MB/s",
        sizeText = "12.4 / 29.6 MB"
    )
}
