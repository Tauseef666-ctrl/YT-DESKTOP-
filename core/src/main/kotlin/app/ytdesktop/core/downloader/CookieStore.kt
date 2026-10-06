/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.downloader

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe cookie storage shared between the extractor [HttpDownloader] and any
 * direct HTTP work (thumbnail loading, media probing, PoToken calls).
 *
 * Kept deliberately small and serialisable-friendly so that, once account support
 * arrives, cookies can be exported/imported as part of the user's profile.
 */
class CookieStore {

    private val byDomain = ConcurrentHashMap<String, MutableList<Cookie>>()

    fun save(cookie: Cookie) {
        val bucket = byDomain.getOrPut(cookie.domain) {
            Collections.synchronizedList(mutableListOf<Cookie>())
        }
        synchronized(bucket) {
            // Replace any existing cookie with the same name + path (RFC 6265 §5.3)
            bucket.removeAll { it.name == cookie.name && it.path == cookie.path }
            bucket.add(cookie)
        }
    }

    fun saveFrom(url: String, setCookieHeaders: List<String>) {
        val httpUrl = url.toHttpUrlOrNull() ?: return
        for (header in setCookieHeaders) {
            runCatching { Cookie.parse(httpUrl, header) }
                .getOrNull()
                ?.let { save(it) }
        }
    }

    fun all(): List<Cookie> = byDomain.values.flatMap { bucket ->
        synchronized(bucket) { bucket.toList() }
    }

    fun cookieHeader(url: String): String = headerFor(url)

    fun headerFor(url: String): String {
        val httpUrl = url.toHttpUrlOrNull() ?: return ""
        return all()
            .filter { it.matches(httpUrl) }
            .joinToString("; ") { "${it.name}=${it.value}" }
    }

    fun removeAll() = byDomain.clear()

    fun exportNetscape(): String = buildString {
        append("# Netscape HTTP Cookie File\n")
        append("# Exported by YT Desktop\n\n")
        for (cookie in all()) {
            val includeSubdomains = if (cookie.hostOnly) "FALSE" else "TRUE"
            val secure = if (cookie.secure) "TRUE" else "FALSE"
            val expiry = if (cookie.expiresAt > 0) cookie.expiresAt else 0
            val domain = if (cookie.hostOnly) cookie.domain.removePrefix(".") else cookie.domain
            append(domain).append('\t')
            append(includeSubdomains).append('\t')
            append(cookie.path).append('\t')
            append(secure).append('\t')
            append(expiry).append('\t')
            append(cookie.name).append('\t')
            append(cookie.value).append('\n')
        }
    }

    internal fun asCookieJar(): CookieJar = object : CookieJar {
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { save(it) }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> =
            all().filter { it.matches(url) }
    }
}