/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.core.service

import app.ytdesktop.core.downloader.HttpDownloader
import app.ytdesktop.core.errors.UserAction
import app.ytdesktop.core.errors.YtException
import app.ytdesktop.core.model.ChannelPage
import app.ytdesktop.core.model.ChannelTab
import app.ytdesktop.core.model.DownloadableStream
import app.ytdesktop.core.model.PagedResult
import app.ytdesktop.core.model.PageItem
import app.ytdesktop.core.model.PlaybackSource
import app.ytdesktop.core.model.ResolvedDownload
import app.ytdesktop.core.model.ResolvedPlayback
import app.ytdesktop.core.model.StreamDetails
import app.ytdesktop.core.model.StreamItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.Page
import org.schabi.newpipe.extractor.StreamingService as NpService
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.stream.VideoStream

/**
 * YouTube front-end over NewPipeExtractor (plan.md 1.1). All extractor access
 * happens on the IO dispatcher and is confined to it, so the shared extractor
 * instances are never touched from UI threads.
 *
 * Stream selection honours risk register R7: AV1-encoded video-only streams
 * are avoided when a VP9/H.264 alternative exists, because the bundled VLC 3.x
 * has no software AV1 decoder and long AV1 fetches stall playback.
 */
class YoutubeService(
    private val downloader: Downloader = HttpDownloader(),
) : StreamingService {

    private val localization = Localization("en", "US")
    private val contentCountry = ContentCountry("US")

    init {
        // Idempotent: NewPipe re-registers its services on each init, cheap and
        // it guarantees our downloader and isolation settings are the active ones.
        NewPipe.init(downloader, localization, contentCountry)
    }

    private val npService: NpService by lazy(LazyThreadSafetyMode.NONE) {
        NewPipe.getServices().firstOrNull { it.baseUrl.contains("youtube", ignoreCase = true) }
            ?: NewPipe.getServiceByUrl("https://www.youtube.com")
    }

    override val name: String get() = "YouTube"
    override val baseUrl: String get() = npService.baseUrl

    override val availableKiosks: List<String> by lazy(LazyThreadSafetyMode.NONE) {
        npService.kioskList.availableKiosks.sorted()
    }

    override suspend fun search(query: String): PagedResult<PageItem> = io(UserAction.SEARCH) {
        val handler = npService.searchQHFactory.fromQuery(query)
        listPage(npService.getSearchExtractor(handler), UserAction.SEARCH)
    }

    override suspend fun suggestions(query: String): List<String> = io(UserAction.SUGGESTIONS) {
        npService.suggestionExtractor.suggestionList(query)
    }

    override suspend fun trending(kioskId: String?): PagedResult<PageItem> = io(UserAction.TRENDING) {
        if (kioskId == null) {
            listPage(npService.kioskList.getDefaultKioskExtractor(), UserAction.TRENDING)
        } else {
            val factory = npService.kioskList.getListLinkHandlerFactoryByType(kioskId)
                ?: throw IllegalArgumentException("Unknown kiosk: $kioskId")
            val handler = factory.fromId(kioskId)
            listPage(npService.kioskList.getExtractorByUrl(handler.url, Page(handler.url)), UserAction.TRENDING)
        }
    }

    override suspend fun channelInfo(url: String): ChannelPage = io(UserAction.CHANNEL) {
        val info = ChannelInfo.getInfo(npService, url)
        ChannelPage(
            title = info.name,
            description = info.description?.takeIf { it.isNotBlank() },
            subscriberCount = info.subscriberCount.takeIf { it > 0 },
            avatarUrl = info.avatars?.asSequence().orEmpty()
                .mapNotNull { it.url.takeIf { u -> u.isNotBlank() } }.firstOrNull(),
            bannerUrl = info.banners?.asSequence().orEmpty()
                .mapNotNull { it.url.takeIf { u -> u.isNotBlank() } }.firstOrNull(),
            tabs = (info.tabs ?: emptyList()).map { tab ->
                ChannelTab(
                    name = displayNameForTab(tab.url),
                    url = tab.url,
                )
            },
        )
    }

    override suspend fun channelTab(url: String): PagedResult<PageItem> = io(UserAction.CHANNEL_TAB) {
        val handler = npService.channelTabLHFactory.fromUrl(url)
        listPage(npService.getChannelTabExtractor(handler), UserAction.CHANNEL_TAB)
    }

    override suspend fun resolvePlayback(videoUrl: String): ResolvedPlayback =
        io(UserAction.STREAM_RESOLUTION) {
            val info = StreamInfo.getInfo(npService, videoUrl)

            // Live broadcasts (user-reported: "live videos are not playing"):
            // a video-only + audio-only split never plays a live broadcast.
            // YouTube's HLS manifest is self-contained (audio + video on one
            // clock), so hand it to VLC whole. NO input-slave for these.
            liveManifestOrNull(info.streamType, info.hlsUrl, info.dashMpdUrl)
                ?.let { manifest ->
                    return@io ResolvedPlayback(
                        video = manifest,
                        audio = null,
                        title = info.name,
                        uploaderName = info.uploaderName,
                        durationSeconds = info.duration,
                        thumbnailUrl = info.thumbnails?.firstOrNull()?.url,
                    )
                }
            if (info.streamType == StreamType.LIVE_STREAM ||
                info.streamType == StreamType.AUDIO_LIVE_STREAM
            ) {
                throw YtException(
                    UserAction.STREAM_RESOLUTION,
                    "This live stream has no playable manifest",
                )
            }

            val video = pickVideoStream(info.videoOnlyStreams)
            val audio = pickAudioStream(info.audioStreams)
            ResolvedPlayback(
                video = PlaybackSource.Remote(
                    url = requireNotNull(video.url) { "Video stream has no URL" },
                    resolution = video.resolution,
                    codec = video.codec,
                    itag = video.itag,
                    mimeType = video.format?.mimeType,
                ),
                audio = PlaybackSource.Remote(
                    url = requireNotNull(audio.url) { "Audio stream has no URL" },
                    codec = audio.codec,
                    itag = audio.itag,
                    mimeType = audio.format?.mimeType,
                ),
                title = info.name,
                uploaderName = info.uploaderName,
                durationSeconds = info.duration,
                thumbnailUrl = info.thumbnails?.firstOrNull()?.url,
            )
        }

    override suspend fun resolveDownload(videoUrl: String): ResolvedDownload =
        io(UserAction.STREAM_RESOLUTION) {
            val info = StreamInfo.getInfo(npService, videoUrl)
            if (info.streamType == StreamType.LIVE_STREAM ||
                info.streamType == StreamType.AUDIO_LIVE_STREAM
            ) {
                throw YtException(
                    UserAction.STREAM_RESOLUTION,
                    "Live broadcasts cannot be downloaded",
                )
            }

            val video = info.videoOnlyStreams
                .filterNot { it.codec?.startsWith("av01") == true }
                .ifEmpty { info.videoOnlyStreams }
                .maxByOrNull { resolutionWidth(it.resolution) ?: 0 }
                ?.let { stream ->
                    val url = requireNotNull(stream.url) { "Video stream has no URL" }
                    DownloadableStream(url, extensionFor(stream.format?.mimeType))
                }
            val audio = info.audioStreams
                .maxByOrNull { it.averageBitrate }
                ?: throw YtException(
                    UserAction.STREAM_RESOLUTION,
                    "This video has no downloadable audio streams",
                )
            ResolvedDownload(
                title = info.name,
                video = video,
                audio = DownloadableStream(
                    requireNotNull(audio.url) { "Audio stream has no URL" },
                    extensionFor(audio.format?.mimeType),
                ),
                durationSeconds = info.duration,
            )
        }

    /** "video/webm; codecs=vp9" -> "webm". Falls back to "bin" when unknown. */
    private fun extensionFor(mimeType: String?): String = when {
        mimeType == null -> "bin"
        "webm" in mimeType || "vp9" in mimeType || "vp8" in mimeType -> "webm"
        "mp4" in mimeType || "avc" in mimeType -> "mp4"
        "m4a" in mimeType || mimeType.startsWith("audio/mp4") -> "m4a"
        "ogg" in mimeType || "opus" in mimeType -> "ogg"
        "3gp" in mimeType -> "3gp"
        else -> "bin"
    }

    override suspend fun streamDetails(url: String): StreamDetails = io(UserAction.VIDEO_PAGE) {
        val info = StreamInfo.getInfo(npService, url)
        StreamDetails(
            title = info.name,
            uploaderName = info.uploaderName,
            uploaderUrl = info.uploaderUrl?.takeIf { it.isNotBlank() },
            durationSeconds = info.duration,
            viewCount = info.viewCount,
            description = info.description.content.takeIf { it.isNotBlank() },
            thumbnailUrl = info.thumbnails?.firstOrNull()?.url?.takeIf { it.isNotBlank() },
            relatedVideos = (info.relatedStreams ?: emptyList())
                .filterIsInstance<StreamInfoItem>()
                .map { StreamItem.fromNewPipe(it) },
        )
    }

    // ----- internals -------------------------------------------------------

    /** Runs [block] on the IO dispatcher and maps failures to [YtException]. */
    private suspend fun <T> io(action: UserAction, block: suspend () -> T): T =
        try {
            withContext(Dispatchers.IO) { block() }
        } catch (e: YtException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw YtException(action, e.message ?: "Unexpected error", e)
        }

    /** First page of [ex]; subsequent pages are lazy via the returned loader. */
    private suspend fun <I : InfoItem> listPage(
        ex: ListExtractor<I>,
        action: UserAction,
    ): PagedResult<PageItem> {
        ex.fetchPage()
        return pageFrom(ex, ex.getInitialPage(), action)
    }

    private fun <I : InfoItem> pageFrom(
        ex: ListExtractor<I>,
        np: ListExtractor.InfoItemsPage<I>,
        action: UserAction,
    ): PagedResult<PageItem> {
        val items = np.items.map(::toPageItem)
        val next = np.nextPage
        if (next == null) return PagedResult.exhausted(items)
        return PagedResult(items) {
            try {
                withContext(Dispatchers.IO) { pageFrom(ex, ex.getPage(next), action) }
            } catch (e: YtException) {
                throw e
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                throw YtException(action, e.message ?: "Unexpected error", e)
            }
        }
    }

    private fun toPageItem(item: InfoItem): PageItem = when (item) {
        is StreamInfoItem -> PageItem.Video(StreamItem.fromNewPipe(item))
        is PlaylistInfoItem -> PageItem.Playlist(
            title = item.name,
            url = item.url,
            thumbnailUrl = item.thumbnails.firstOrNull()?.url?.takeIf { it.isNotBlank() },
            uploaderName = item.uploaderName,
            streamCount = item.streamCount,
        )
        is ChannelInfoItem -> PageItem.Channel(
            title = item.name,
            url = item.url,
            thumbnailUrl = item.thumbnails.firstOrNull()?.url?.takeIf { it.isNotBlank() },
            subscriberCount = item.subscriberCount,
        )
        else -> PageItem.Channel(
            title = item.name,
            url = item.url,
            thumbnailUrl = null,
            subscriberCount = null,
        )
    }

    /**
     * Pure live-broadcast resolution rule, offline-testable: a live stream is
     * a self-contained manifest (HLS preferred, DASH fallback). Returns null
     * when [streamType] is not a live variant or no manifest is available.
     */
    internal fun liveManifestOrNull(
        streamType: StreamType,
        hlsUrl: String?,
        dashMpdUrl: String?,
    ): PlaybackSource.Remote? {
        if (streamType != StreamType.LIVE_STREAM &&
            streamType != StreamType.AUDIO_LIVE_STREAM
        ) {
            return null
        }
        val manifest = hlsUrl ?: dashMpdUrl ?: return null
        return PlaybackSource.Remote(
            url = manifest,
            resolution = "LIVE",
            codec = if (manifest.endsWith(".m3u8")) "HLS" else "DASH",
        )
    }

    // R7: avoid AV1 video-only streams when a VP9/H.264 alternative exists.
    private fun pickVideoStream(streams: List<VideoStream>): VideoStream {
        if (streams.isEmpty()) {
            throw YtException(UserAction.STREAM_RESOLUTION, "This video has no playable video streams")
        }
        val coded = streams.filterNot { it.codec?.startsWith("av01") == true }.ifEmpty { streams }
        return coded.maxByOrNull { resolutionWidth(it.resolution) ?: 0 } ?: coded.first()
    }

    private fun pickAudioStream(streams: List<AudioStream>): AudioStream {
        if (streams.isEmpty()) {
            throw YtException(UserAction.STREAM_RESOLUTION, "This video has no playable audio streams")
        }
        return streams.maxByOrNull { it.averageBitrate } ?: streams.first()
    }

    /** "1080p60" -> 1080, "256x144" -> 256, "4320p" -> 4320. */
    private fun resolutionWidth(resolution: String?): Int? =
        resolution?.let { Regex("""(\d{3,4})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }

    /** Channel tab handlers carry no display name, so derive it from the URL. */
    private fun displayNameForTab(url: String): String {
        val raw = url.trimEnd('/').substringAfterLast('/')
        if (raw.isBlank()) return "Videos"
        return raw.replace('-', ' ')
            .split(' ')
            .joinToString(" ") { it.replaceFirstChar(Char::uppercaseChar) }
    }
}