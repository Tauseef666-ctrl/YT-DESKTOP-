/*
 * YT Desktop — an independent YouTube front-end for Windows and Android.
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package app.ytdesktop.player

import app.ytdesktop.core.downloader.HttpDownloader
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Timeout
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.stream.StreamInfo
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Spike **S3** — libVLC playing the things YT Desktop actually needs to play.
 *
 * Three questions, one gate:
 *  1. Can VLC 3.0.21 demux and decode YouTube's **DASH** manifest? (R3, R7)
 *  2. Does it also manage **HLS**, so we have a fallback?
 *  3. Can it play a **local `.mp4` with an `.srt` sidecar**? (Phase 2.6/2.7)
 *
 * Playback runs headless (`--vout=dummy --aout=dummy`) so it works on a CI
 * runner with no display. That proves demux + decode; the Compose surface
 * itself is exercised by the running app, not here.
 *
 * Requires the libVLC tree staged by the `vlcSetup` Gradle task — see
 * `tasks.test` in `desktopApp/build.gradle.kts`, which points
 * `jna.library.path` and `vlc.plugin.path` at it.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PlaybackSpikeTest {

    private lateinit var factory: MediaPlayerFactory
    private var vlcLog: File? = null

    @BeforeAll
    fun createFactory() {
        val vlcHome = System.getProperty("yt.vlcHome")
            ?: error("yt.vlcHome is not set - run this test through Gradle, not the IDE")
        vlcLog = File(System.getProperty("java.io.tmpdir"), "yt-desktop-s3-vlc.log").apply {
            delete()
            absolutePath
        }
        println("S3 vlcHome=$vlcHome")
        println("S3 vlcLog=$vlcLog")

        factory = MediaPlayerFactory(
            // No display and no audio device on a CI runner.
            "--vout=dummy",
            "--aout=dummy",
            "--no-video-title-show",
            "--no-osd",
            "--no-stats",
            // The staged plugin set is filtered per build, so never trust a cache.
            "--no-plugins-cache",
            // Never silence it: "DASH errored" with no message cost a full
            // 10-minute cycle to discover. We want the reason.
            "--verbose=2",
            "--file-logging",
            "--logfile=$vlcLog",
        )
    }

    /** Dumps the tail of libVLC's own log, which is where the real reason is. */
    private fun dumpVlcLog(reason: String) {
        println("--- libVLC log ($reason) : $vlcLog ---")
        val lines = vlcLog?.takeIf { it.isFile }?.readLines().orEmpty()
        if (lines.isEmpty()) {
            println("  (empty - libVLC never logged anything)")
        } else {
            lines.takeLast(40).forEach { println("  $it") }
        }
        println("--- end libVLC log ---")
    }

    @AfterAll
    fun releaseFactory() {
        if (this::factory.isInitialized) factory.release()
    }

    @Test
    @Timeout(90)
    fun `remote DASH manifest plays`() {
        val dashUrl = extract().dashMpdUrl
        assertNotNull(dashUrl, "extractor returned no DASH MPD - nothing to play")
        println("S3 DASH: $dashUrl")

        val result = playAndProbe(dashUrl)

        println(
            "  playing=${result.reachedPlaying} advanced=${result.advancedMs}ms " +
                "length=${result.lengthMs}ms video=${result.videoWidth}x${result.videoHeight} " +
                "outputs=${result.videoOutputs} error=${result.error}",
        )

        assertTrue(result.reachedPlaying && !result.error, "DASH never reached the playing state")
        assertTrue(
            result.advancedMs > 1_500,
            "DASH playback did not advance (stalled at ${result.advancedMs} ms)",
        )
        assertTrue(
            result.videoWidth > 0 && result.videoHeight > 0,
            "DASH audio played but no video was decoded (${result.videoWidth}x" +
                "${result.videoHeight}) - this is the AV1/codec risk R7",
        )
    }

    @Test
    @Timeout(90)
    fun `remote HLS manifest plays`() {
        val hlsUrl = extract().hlsUrl
        assertNotNull(hlsUrl, "extractor returned no HLS manifest - no DASH fallback available")
        println("S3 HLS: $hlsUrl")

        val result = playAndProbe(hlsUrl)

        println(
            "  playing=${result.reachedPlaying} advanced=${result.advancedMs}ms " +
                "length=${result.lengthMs}ms video=${result.videoWidth}x${result.videoHeight} " +
                "outputs=${result.videoOutputs} error=${result.error}",
        )

        assertTrue(result.reachedPlaying && !result.error, "HLS never reached the playing state")
        assertTrue(
            result.advancedMs > 1_500,
            "HLS playback did not advance (stalled at ${result.advancedMs} ms)",
        )
    }

    @Test
    @Timeout(60)
    fun `local mp4 plays and picks up its sidecar srt`() {
        val dir = File(System.getProperty("java.io.tmpdir"), "yt-desktop-s3").apply { mkdirs() }
        // Same basename in the same directory is how VLC auto-discovers sidecars.
        copyResource("sample.mp4", File(dir, "sample.mp4"))
        copyResource("sample.srt", File(dir, "sample.srt"))

        val result = playAndProbe(File(dir, "sample.mp4").absolutePath)

        println(
            "  playing=${result.reachedPlaying} advanced=${result.advancedMs}ms " +
                "length=${result.lengthMs}ms video=${result.videoWidth}x${result.videoHeight} " +
                "subtitles=${result.subtitleTracks} error=${result.error}",
        )

        assertTrue(result.reachedPlaying && !result.error, "local mp4 never reached the playing state")
        assertTrue(
            result.advancedMs > 1_500,
            "local mp4 playback did not advance (stalled at ${result.advancedMs} ms)",
        )
        assertTrue(
            result.videoWidth > 0 && result.videoHeight > 0,
            "local mp4 produced no video output - the bundled codec set is wrong",
        )
        assertTrue(
            result.subtitleTracks > 0,
            "the .srt sidecar was not picked up (0 subtitle tracks)",
        )
    }

    // ---------------------------------------------------------------- helpers

    private fun extract(): StreamInfo {
        NewPipe.init(HttpDownloader(), Localization("en", "US"))
        val service = NewPipe.getServices()
            .first { it.baseUrl.contains("youtube.com", ignoreCase = true) }
        return StreamInfo.getInfo(service, KNOWN_VIDEO)
    }

    private data class Probe(
        val reachedPlaying: Boolean,
        val advancedMs: Long,
        val lengthMs: Long,
        val videoWidth: Int,
        val videoHeight: Int,
        val videoOutputs: Int,
        val subtitleTracks: Int,
        val error: Boolean,
    )

    private fun playAndProbe(location: String): Probe {
        val player: EmbeddedMediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer()

        val started = CountDownLatch(1)
        val failed = CountDownLatch(1)
        player.events().addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: MediaPlayer) {
                started.countDown()
            }

            override fun error(mediaPlayer: MediaPlayer) {
                failed.countDown()
            }
        })

        try {
            player.media().play(location)

            val reachedPlaying = started.await(25, TimeUnit.SECONDS)
            val errored = failed.count == 0L
            if (!reachedPlaying || errored) {
                dumpVlcLog("did not reach the playing state")
                return Probe(false, 0, 0, 0, 0, 0, 0, errored)
            }

            // Wait for the clock to move, not just for the state to change: a
            // stream that opens and then stalls is the failure we care about.
            val deadline = System.currentTimeMillis() + 15_000
            var best = 0L
            while (System.currentTimeMillis() < deadline) {
                best = maxOf(best, player.status().time())
                if (best >= 2_000) break
                if (failed.count == 0L) break
                Thread.sleep(200)
            }

            val dimension = player.video().videoDimension()
            val probe = Probe(
                reachedPlaying = true,
                advancedMs = best,
                lengthMs = player.status().length(),
                videoWidth = dimension?.width ?: 0,
                videoHeight = dimension?.height ?: 0,
                videoOutputs = player.status().videoOutputs(),
                subtitleTracks = player.subpictures().trackCount(),
                error = failed.count == 0L,
            )
            if (probe.error || probe.advancedMs < 1_500) {
                dumpVlcLog("stalled at ${probe.advancedMs}ms")
            }
            return probe
        } finally {
            player.release()
        }
    }

    private fun copyResource(name: String, target: File) {
        val stream = checkNotNull(javaClass.getResourceAsStream("/$name")) { "missing fixture $name" }
        stream.use { input -> target.outputStream().use { input.copyTo(it) } }
    }

    private companion object {
        /** Globally available, never removed, not region locked. */
        const val KNOWN_VIDEO = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    }
}
