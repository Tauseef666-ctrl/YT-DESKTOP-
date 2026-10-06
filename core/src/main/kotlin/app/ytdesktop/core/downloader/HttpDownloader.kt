/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.brotli.BrotliInterceptor
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.util.concurrent.TimeUnit

/**
 * OkHttp-backed [Downloader] for NewPipeExtractor.
 *
 * The extractor at our pinned commit declares exactly one abstract member,
 * `execute(Request)`. Everything else (`get`, `head`, `post`,
 * `postWithContentTypeJson`, …) is concrete and funnels through it, so this
 * class only has to translate extractor [Request]s into OkHttp calls and back.
 *
 * Note we deliberately do *not* override `getAsStream`/`getContentLength`:
 * those were removed upstream. Video bytes never flow through the extractor —
 * they go straight from libVLC or the download engine to disk — so a streaming
 * downloader is unnecessary here. See [contentLength] for our own equivalent.
 */
class HttpDownloader(
    val cookieStore: CookieStore = CookieStore(),
    private val userAgent: String = DEFAULT_USER_AGENT,
    clientBuilder: OkHttpClient.Builder = OkHttpClient.Builder(),
) : Downloader() {

    val client: OkHttpClient = clientBuilder
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .cookieJar(cookieStore.asCookieJar())
        // okhttp decompresses gzip transparently when the caller does not pin
        // Accept-Encoding itself; BrotliInterceptor covers `br`. Note that
        // okhttp 4.x has no CompressionInterceptor/Gzip/Brotli API — those only
        // exist in okhttp 5.x, which would drag the whole toolchain forward.
        .addInterceptor(BrotliInterceptor)
        .build()

    override fun execute(request: Request): Response {
        val payload = request.dataToSend()?.toRequestBody(null)

        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), payload)

        val cookies = cookieStore.headerFor(request.url())
        if (cookies.isNotEmpty()) {
            builder.header("Cookie", cookies)
        }
        builder.header("User-Agent", userAgent)

        // Caller-supplied headers (e.g. Accept-Language from Localization) win.
        request.headers()?.forEach { (name, values) ->
            builder.removeHeader(name)
            values.forEach { value -> builder.addHeader(name, value) }
        }

        client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", request.url())
            }

            val setCookies = response.headers.values("Set-Cookie")
            if (setCookies.isNotEmpty()) {
                cookieStore.saveFrom(request.url(), setCookies)
            }

            val body = response.body?.string()
            val headers = response.headers.toMultimap()
            val latestUrl = response.request.url.toString()

            return Response(
                response.code,
                response.message,
                headers,
                body,
                latestUrl,
            )
        }
    }

    /**
     * Size in bytes of the resource behind [url], or `null` when the server does
     * not answer a HEAD request usefully. Used to show total size / progress for
     * downloads before they start.
     */
    suspend fun contentLength(url: String): Long? = withContext(Dispatchers.IO) {
        val httpUrl = url.toHttpUrlOrNull() ?: return@withContext null
        val head = okhttp3.Request.Builder()
            .url(httpUrl)
            .head()
            .header("User-Agent", userAgent)
            .build()

        runCatching {
            client.newCall(head).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull()
            }
        }.getOrNull()
    }

    companion object {
        /**
         * Desktop-class browser UA. A current Firefox UA is used because YouTube
         * serves a much richer response set to it than to headless/Chrome-less
         * clients. Update this periodically; a stale UA is a common cause of
         * sudden extractor breakage.
         */
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
    }
}