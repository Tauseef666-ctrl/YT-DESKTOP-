/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.ui.update

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.ytdesktop.core.downloader.HttpDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The cross-platform update layer (Phase 1 add-on, shared by Windows, Android
 * and Android TV).
 *
 * The desktop app is wired today; the Android/TV apps consume the same `ui`
 * module later, so every target gets the same "check for a newer build, here it
 * is, grab it" experience against our GitHub Releases.
 */

/** How the current build identifies itself on its platform. */
data class AppInfo(
    val currentVersion: String,
    val platformLabel: String,
)

/** One downloadable artifact inside a GitHub release. */
data class UpdateAsset(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long,
) {
    /** Whether this artifact fits the given platform label (Windows/Android/TV). */
    fun matches(platformLabel: String): Boolean {
        val label = platformLabel.lowercase()
        return when {
            label.contains("windows") -> name.endsWith(".msi") || name.endsWith(".exe")
            label.contains("tv") -> name.endsWith(".apk") && name.contains("tv", ignoreCase = true)
            else -> name.endsWith(".apk") && !name.contains("tv", ignoreCase = true)
        }
    }
}

/** The latest published release, as reported by the GitHub releases API. */
data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val publishedAt: String,
    val body: String,
    val assets: List<UpdateAsset>,
) {
    /** Version tags are `"0.2.0"` or `"v0.2.0"`; compare normalized. */
    val version: String
        get() = tagName.removePrefix("v").removePrefix("V")

    fun isNewerVersionThan(current: String): Boolean = version != current
}

sealed class UpdateState {
    data object Checking : UpdateState()
    data class UpToDate(val currentVersion: String, val latestVersion: String) : UpdateState()
    data class Available(val currentVersion: String, val release: ReleaseInfo) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

/**
 * Fetches `releases/latest` for our repo. Returns null when no release has
 * been published yet. Runs the network call on the IO dispatcher.
 */
suspend fun checkLatestRelease(owner: String = REPO_PATH): ReleaseInfo? = withContext(Dispatchers.IO) {
    val request = okhttp3.Request.Builder()
        .url("https://api.github.com/repos/${owner}/releases/latest")
        .header("User-Agent", HttpDownloader.DEFAULT_USER_AGENT)
        .header("Accept", "application/vnd.github+json")
        .build()

    HttpDownloader().client.newCall(request).execute().use { response ->
        when {
            response.code == 404 -> null // no release published yet
            !response.isSuccessful -> throw IllegalStateException("GitHub API returned HTTP ${response.code}")
            else -> parseRelease(response.body?.string().orEmpty())
        }
    }
}

/** Lenient parser — we only need the receiver's own release shape. */
internal fun parseRelease(json: String): ReleaseInfo? {
    val tag = field(json, "tag_name") ?: return null
    val body = field(json, "body").orEmpty()
    return ReleaseInfo(
        tagName = tag,
        title = field(json, "name").orEmpty(),
        publishedAt = field(json, "published_at").orEmpty(),
        body = unescape(body),
        assets = assetsOf(json),
    )
}

private fun field(json: String, key: String): String? =
    Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(json)?.groupValues?.get(1)

private fun unescape(raw: String): String = raw
    .replace("\\n", "\n")
    .replace("\\r", "")
    .replace("\\\"", "\"")
    .replace("\\t", "\t")
    .replace("\\/", "/")

/** Extracts the `assets` array's objects (they have no nested braces). */
private fun assetsOf(json: String): List<UpdateAsset> {
    val block = Regex("\"assets\"\\s*:\\s*\\[(.*)]", RegexOption.DOT_MATCHES_ALL)
        .find(json)?.groupValues?.get(1) ?: return emptyList()
    return Regex("\\{[^{}]*}", RegexOption.DOT_MATCHES_ALL)
        .findAll(block).mapNotNull { obj ->
        val name = field(obj.value, "name") ?: return@mapNotNull null
        val url = field(obj.value, "browser_download_url") ?: return@mapNotNull null
        val size = Regex("\"size\"\\s*:\\s*(\\d+)").find(obj.value)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        UpdateAsset(name, url, size)
    }.toList()
}

/**
 * The "keep you on the newest build" pane. [openUrl] is platform-provided
 * (desktop opens the default browser; Android/TV would defer to an intent).
 */
@Composable
fun UpdatePane(
    info: AppInfo,
    openUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var attempt by remember { mutableIntStateOf(0) }
    val state by produceState<UpdateState>(UpdateState.Checking, info, attempt) {
        value = UpdateState.Checking
        value = runCatching {
            when (val release = checkLatestRelease()) {
                null -> UpdateState.UpToDate(info.currentVersion, info.currentVersion)
                else -> if (release.isNewerVersionThan(info.currentVersion)) {
                    UpdateState.Available(info.currentVersion, release)
                } else {
                    UpdateState.UpToDate(info.currentVersion, release.version)
                }
            }
        }.getOrElse { UpdateState.Error(it.message ?: "Could not check for updates") }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Updates", style = MaterialTheme.typography.headlineSmall)
        Text(
            "${info.currentVersion} · ${info.platformLabel}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        when (val s = state) {
            is UpdateState.Checking -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) { CircularProgressIndicator(Modifier.height(20.dp)); Text("Checking releases…") }

            is UpdateState.UpToDate -> Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("You're up to date", fontWeight = FontWeight.SemiBold)
                    Text(
                        "No newer build than ${s.currentVersion} is published.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            is UpdateState.Available -> {
                Text(
                    "New build ${s.release.version} is available",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    s.release.publishedAt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (s.release.body.isNotBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("What's new", fontWeight = FontWeight.SemiBold)
                            Text(s.release.body, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                val assets = s.release.assets.filter { it.matches(info.platformLabel) }
                if (assets.isNotEmpty()) {
                    Text("Download for ${info.platformLabel}", style = MaterialTheme.typography.titleSmall)
                    assets.forEach { asset ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(Modifier.weight(1f)) {
                                Text(asset.name, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(
                                    formatBytes(asset.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Button(onClick = { openUrl(asset.downloadUrl) }) { Text("Download") }
                        }
                    }
                } else {
                    OutlinedButton(onClick = { openUrl("https://github.com/${REPO_PATH}/releases") }) {
                        Text("Open releases page")
                    }
                }
            }

            is UpdateState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Couldn't check for updates", color = MaterialTheme.colorScheme.error)
                Text(
                    s.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = { attempt++ }) { Text("Retry") }
                Spacer(Modifier.padding(4.dp))
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

internal const val REPO_PATH = "Tauseef666-ctrl/YT-DESKTOP-"