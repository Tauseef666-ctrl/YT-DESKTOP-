# YT Desktop — Build Plan

> **Status:** Phase 1 — Browse / Search / Play (in progress)
> **License:** GPL-3.0-or-later
> **Outputs:** Windows `.msi`/`.exe` (Compose Desktop) + Android `.apk`, both built by GitHub Actions
> **This file is the single source of truth for build progress.** Update it after every completed item.

---

## Attribution

This project is a clean-room-inspired reimplementation inspired by **NewPipe** (Android) by
NewPipe e.V. <https://newpipe-ev.de> and **NewPipeExtractor** by TeamNewPipe
<https://github.com/TeamNewPipe/NewPipeExtractor>, both GPL-3.0-or-later.

Bundles **libVLC** (LGPL-2.1-or-later) by VideoLAN.

"NewPipe" is a registered word mark of NewPipe e.V. **This project is not affiliated with,
endorsed by, or named NewPipe.** See <https://newpipe-ev.de/policy/trademark/>.

---

## Prerequisites

| Item | Status | Notes |
|---|---|---|
| JDK 21 (Temurin) | ✅ done | `21.0.12.1+1 LTS` at `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot` |
| Gradle 8.13 wrapper | ✅ generated | `gradlew` works; distribution seeded into `~/.gradle/wrapper/dists` |
| GitHub CLI `gh` | ✅ authenticated | Logged in as `Tauseef666-ctrl`; CI logs readable via `gh run view <id> --log-failed` |
| Disk headroom | ⚠️ 13.8 GB free | Hard checkpoint: purge caches if < 5 GB |
| Android SDK | ⏳ only needed for the APK build | CI installs it; local desktop build does not need it |

---

## Phase 0 — Toolchain + Spikes (~400 lines)

- [x] **0.1** Install JDK 21, verify `java -version`
- [x] **0.2** Scaffold Gradle project — settings, root build, version catalog, wrapper 8.13
- [x] **0.3** Compose Desktop shell, adaptive layout (3 pane widths) — compiles
- [x] **0.4** VLCJ 4.11.0 video surface — `VlcPlayer` + `SwingPanel` host, compiles
- [x] **0.5** `ir.mahozad.vlc-setup` + bundle libVLC into the installer — verified by CI
- [x] **0.6** `packageMsi` + `packageExe` → ✅ **green CI run**
- [x] **0.7** GitHub Actions workflow for MSI **and** APK — ✅ **all jobs green**
- [x] **0.8** Android TV build — `tv` flavour, leanback packaging, D-pad shell, CI artifact

> **Verification status:** ✅ Run `37423534342` is green on both jobs.
> Artifacts: `windows-installer` (235 MB MSI+EXE), `android-apk` (10.9 MB phone
> debug), `android-tv-release` (8.1 MB TV release). Android 112 s, Windows 196 s,
> ~3m16s wall clock.
>
> Remaining Phase 0 work is spikes **S1–S3** (0.4 video surface is done).

### Spikes — ALL must pass before Phase 1 starts

- [x] **S1** YouTube search + stream resolution from a plain JVM (no WebView) — ✅ `YoutubeExtractionSpikeTest` green locally and enforced as the `spikes` CI job. Search returned 20 items + next page; stream extraction returned 5 audio streams, a DASH MPD and an HLS manifest; a ranged GET on a `googlevideo.com` URL returned **HTTP 206 with 1024 bytes**. No PoToken and no cookies were needed.
  - **Finding:** `videoStreams = 0` — YouTube serves *no* muxed progressive stream to this client, so remote playback **must** go through DASH (separate audio + video) or the DASH/HLS manifest. Feeds R7 and the shape of Phase 1.6.
  - **Finding:** `NewPipe.getServiceByUrl()` only accepts URLs that look like a *stream* link; a bare homepage throws `ExtractionException`. Select the service by `baseUrl` instead.
- [ ] **S2** PoToken via `bgutil-pot.exe` subprocess (cookies **OFF**) — urgency dropped: S1 resolved streams and served bytes without one. Still required to confirm *when* it becomes necessary (region, format, quota).
- [x] **S3** libVLC playback of what the app ships: direct `googlevideo.com/videoplayback` stream URLs (video **and** audio) plus a local `.mp4`+`.srt` — ✅ `PlaybackSpikeTest` green on the clean Windows runner (run `37437100182`): video-only 144p advanced 2150 ms, decoded 256×144, 1 video output; audio (itag 139) advanced 2151 ms; local mp4+srt advanced 2402 ms at 480×270 with 2 subtitle tracks. Bundled VLC 3.0.21 demuxes YouTube DASH fMP4 directly — **no manifest parsing needed**. Runs as the `S3 - libVLC playback spike` step of the Windows CI job (`:desktopApp:test`, `dependsOn vlcSetup`).
  - **Finding:** VLC 3.x **cannot** claim YouTube's extension-less `/api/manifest/hls_variant|dash` URLs — they fall to the `ps` demuxer probe and stall at 0 ms (`MOD validation failed`). Manifest-based playback is a dead end; direct per-stream URLs are the production path (they come free out of `StreamInfo`).
  - **Findings:** `desktopApp/src/main/appResources/*` is output of the vlc-setup plugin, so it is **git-ignored**; `shouldIncludeAllVlcFiles` must stay `true`; `shouldCompressVlcFiles` (UPX) disabled during spikes, re-enable before shipping; dev-machine WDAC policy (error 4551) corrupts local libVLC runs, so S3 is CI-only.

---

## Phase 1 — Browse / Search / Play (~7K lines)

- [x] **1.1** Service layer — pluggable `StreamingService` interface, YouTube impl (`core/service`, slice A, CI-verified: search / suggestions / trending / resolve playback)
- [x] **1.2** OkHttp `Downloader` + Brotli + cookie jar (done in Phase 0, exercised by slice A)
- [x] **1.3** Trending kiosks (8 YouTube kiosk IDs) + kiosk picker UI — `TrendingScreen` (slice B): runtime kiosk chips, feed swapper, self-paging feed
- [x] **1.4** Search + live suggestions + history — `SearchScreen` (slices B/C): debounced suggestions, Enter-to-search, self-paging feed; recent-search persistence (most-recent-first, deduped, capped, clearable) via `SearchHistoryStore`
- [x] **1.5** Channel page (7 playable tabs) — `channelInfo`/`channelTab` done (slice A, `ChannelPage`/`ChannelTab`); `ChannelScreen` (slice B part 2): header (avatar/subscribers/description), tab chips auto-selecting the first as "Videos", per-tab self-paging feed; channel rows in Trending/Search feed open it via `onChannelClick`
- [x] **1.6** Unified player — `PlaybackSource` seam + `PlayerEngine` contract done (slice A); `VlcPlayerEngine` (slice C) plays a video master with its DASH audio slave (`input-slave`) on one clock, `PlayerChrome` drives it (tap-to-seek, transport); S3 pair test gate added
- [ ] **1.7** Play queue (4 pagination strategies) — `PagedResult` continuation loader + `ResourceFeed` paging UI done; queue controller + player integration pending (slice C follow-up)
- [x] **1.8** Error taxonomy (`ErrorInfo` / `UserAction` parity) — `core/errors` done (slice A); UI surfaces it (slice D): `ErrorCard` renders action + stable message + retry in every feed, `ChannelScreen` and the player chrome consume the same taxonomy; `UserAction.UNKNOWN` catch-all added
- [x] **1.9** Update layer — cross-platform (Windows/Android/TV): checks GitHub `releases/latest`, parses per-platform assets, what's-new notes + download/releases buttons. Desktop wired via `NavigationPane` + `UpdatePane`; `UpdateLayerParserTest` covers parsing offline; Android/TV reuse the same `ui` composables later

---

## Phase 2 — Downloads + Local Library (~8K lines)

- [ ] **2.1** Port download engine (512 KiB blocks, 3 worker threads, resume offsets)
- [ ] **2.2** HTTP 403 → re-extract stream URL recovery
- [ ] **2.3** Port muxers (`Mp4FromDashWriter`, `WebMWriter`, `OggFromWebMWriter`, `SrtFromTtmlWriter`)
- [ ] **2.4** Download queue UI + tray progress
- [ ] **2.5** Local library scan (folders as playlists)
- [ ] **2.6** In-app playback of downloaded files
- [ ] **2.7** `.srt` sidecar subtitle auto-load

---

## Phase 3+ — Deferred (not in current scope)

Playlists · bookmarks · subscriptions + channel groups · feed · history · comments ·
settings parity · backup/restore · import/export.

---

## Android TV

Packaging and the D-pad shell are done (0.8). The rest is a backlog lifted from
NewPipe's TV work so we ship the fixes its users keep asking for.

Reference: TeamNewPipe/NewPipe **PR #2806** "Android TV support" (merged) and its
`AndroidTvUtils`, `FocusOverlayView`, `FocusAwareSeekBar`, `NewPipeRecyclerView`,
`LargeTextMovementMethod`.

- [x] `LEANBACK_LAUNCHER`, `android:banner`, touchscreen `required=false`
- [x] Runtime `isTv()` + confirm/direction key classification
- [x] Leanback landing shell: every target D-pad reachable, focus ring at distance
- [x] Landscape pinned + screen kept on while watching (TV boxes refuse portrait — #2806)
- [ ] Player D-pad contract: DPAD shows/hides controls, BACK dismisses controls before exiting
- [ ] Focus indicator overlay for the player surface
- [ ] D-pad seek bar with visible thumb and repeat-on-hold
- [ ] Long descriptions scrollable with the D-pad
- [ ] Audit every screen for gesture-only actions — NewPipe #3687, #11197, #13853
- [ ] Tab reordering without swipe gestures — NewPipe #12635
- [ ] Popup/player resize with keys — NewPipe #12823

---

## Module Layout

```
core/        pure Kotlin/JVM — no Compose, no Android. Produces a JAR that both
             the desktop app and the Android app consume.
  service/     pluggable StreamingService abstraction (YouTube impl)
  downloader/  extractor Downloader over OkHttp
  model/       PlayQueueItem, PlaybackSource, StreamItem
  player/      PlayerEngine interface + PlaybackState machine
  download/    download engine + postprocessing/muxers
  streams/     pure-JVM container parsers/writers ported from NewPipe
  settings/    config model

ui/          Kotlin/JVM + Compose Multiplatform **desktop** target — shared desktop
             UI. Deliberately NOT a KMP module with androidTarget() yet: adding
             androidTarget() would force AGP to be resolved for every desktop
             build. Android currently has its own thin Compose UI in androidApp;
             the shared-UI question is revisited in Phase 1.
desktopApp/  Compose Desktop JVM app → .msi / .exe
androidApp/  Android application → .apk (only in settings when -Pyt.android=on)
```

---

## Decisions Log

| Decision | Rationale |
|---|---|
| App name "YT Desktop" | "NewPipe" is a registered word mark; infringing names are not tolerated |
| YouTube-only | Scope decision. Service layer stays **pluggable** so adding services later is one class |
| Ship libVLC bundled | LGPL requires users be able to replace the DLLs; they ship in a `libvlc/` subfolder with their license |
| PoToken via `bgutil-pot.exe` | Android solves this with a `WebView`; Windows can run BotGuard JS in a 10–15 MB binary instead |
| PoToken **and** cookies are mutually exclusive | Verified landmine: combining them makes YouTube return *"Requested format is not available"* |
| `bgutil` recommended in `settings.gradle.kts` as `includeBuild` | Optional; enable only when extractor work requires a local checkout |
| KMP only for `ui`, plain JVM for `core` | Core is pure JVM bytecode so Android can consume it directly without an `expect/actual` split |
| Android TV is a **flavour, not a fork** | `tv`/`phone` share every source file; only the manifest overlay and banner differ. Mirrors NewPipe's merged TV support (#2806), which added `LEANBACK_LAUNCHER` + `TvUtils.isTv()` instead of forking |
| TV behaviour decided at **runtime** | One APK then also runs on a TV box; `TvUtils` is a port of NewPipe's `AndroidTvUtils` (`isTv`, `isConfirmKey`, `isDirectionKey`) |
| Release APK signed with the **debug keystore** | Keeps the CI artifact installable. Replace before publishing |
| R8 **off** for release for now | Needs a ProGuard ruleset for NewPipeExtractor + kotlinx.serialization that cannot be exercised without a local Android SDK. Tracked as 0.8 follow-up |
| NewPipeExtractor pinned to a **39-char commit prefix** | JitPack purges artifacts; the exact 40-char SHA-1 404s while the prefix still resolves to the same commit — the same workaround NewPipe's own `libs.versions.toml` documents |
| libVLC seeded from `download.videolan.org` in CI | `get.videolan.org` 302s to a mirror pool and `mirror.ajl.albony.in` served an **expired TLS cert**; a cert failure cannot be retried, so the archive is fetched from VideoLAN's own host before Gradle runs |
| Player controls sit **below** the video stage, not over it | Compose Desktop's `SwingPanel` renders above the Skia layer, so a Compose overlay would be painted *underneath* the video. Controls live in their own row until S3 proves an overlay is possible |

---

## Risk Register

| ID | Risk | Status | Mitigation |
|---|---|---|---|
| **R1** | YouTube extraction from a plain JVM | 🟢 handled | Spike **S1** green: search + stream extraction + byte-level probe (HTTP 206) with no WebView, no PoToken, no cookies. Enforced by the `spikes` CI job |
| **R2** | PoToken requirement | 🔴 open | Solution known — gated by spike **S2** |
| **R3** | libVLC DASH/HLS + subtitle rendering | 🟢 handled | Spike **S3** green on the clean Windows runner: direct video+audio `googlevideo.com/videoplayback` URLs and a local mp4+srt all advance and decode. HLS/manifest paths are a non-feature (VLC 3.x never claims extension-less manifest URLs) |
| **R4** | YouTube OTF / post-live DVR manifest synthesis has no libVLC equivalent | 🟡 accepted | Feed libVLC direct progressive/HLS URLs; accept fidelity gap |
| **R5** | 13.8 GB disk headroom | 🟡 monitoring | Hard checkpoint: purge Gradle caches below 5 GB |
| **R6** | **YouTube-only means no fallback service.** If YouTube breaks, there is nothing else to show | 🟡 accepted | Keep `StreamingService` pluggable so recovery is cheap |
| **R7** | **libVLC 3.x cannot decode AV1.** YouTube defaults to AV1/VP9; VLC 3.0.21 covers VP8/VP9/H.264/H.265 + audio but *not* AV1 or H.266 | 🟡 monitoring | S3 confirmed the mitigation shape: pick a decoded format (VP9/H.264) before handing the URL to VLC. 144p AVC decoded fine headless. Still unproven: **high-resolution** AV1 is served first by YouTube, so stream selection must resolve AV1-explicit itags to a VP9/H.264 variant in Phase 1.6 |
| **R8** | No local build possible — `dl.google.com` unreachable, Maven Central crawl rate. Nothing has been compiled | 🔴 open | Verify via GitHub Actions on every push; do not trust "written" code until CI is green |
| **R9** | Extractor API drift: at the pinned commit `Downloader.execute(Request)` is the only abstract member (`getAsStream`/`getContentLength` were removed upstream) | 🟢 handled | `HttpDownloader` implements only `execute`; re-check on every extractor bump |
| **R10** | **JitPack purges published artifacts.** The pinned NewPipeExtractor SHA-1 returned 404 mid-project | 🟢 handled | Pinned a 39-char prefix of the same commit; NewPipe's own catalog documents this workaround |
| **R11** | Third-party CDN/mirror reliability for the 77 MB libVLC archive (truncated body, then an expired TLS cert) | 🟢 handled | CI seeds from `download.videolan.org` with a size check; `Download` tasks retry + `tempAndMove` |
| **R12** | Android TV UX parity: NewPipe still tracks gesture-only actions as broken on TV (#3687, #11197, #13853, #12635) | 🟡 open | Every control we ship must have a D-pad equivalent; `TvShell` sets the pattern, audit when the player lands |

---

## Cleanup Checklist (execute at end, WITH user confirmation)

- [ ] Delete Gradle caches (`~/.gradle/caches`) — ~2–4 GB
- [ ] Delete downloaded libVLC binaries — ~1 GB
- [ ] Delete `build/` outputs
- [ ] **Keep** JDK 21 + Gradle wrapper — required to rebuild or patch

> Note: deleting the JDK makes the project unbuildable. Recommended to keep it.

---

## Progress Log

| Date | Item | Notes |
|---|---|---|
| 2026-10-05 | 0.1 | JDK 21.0.12.1+1 LTS installed via winget, added to user `PATH` and `JAVA_HOME` |
| 2026-10-05 | 0.2 | `settings.gradle.kts`, root `build.gradle.kts`, `gradle.properties`, `libs.versions.toml`, wrapper 8.13. Extractor + OkHttp + Kotlin/Compose plugins all resolved from cache |
| 2026-10-05 | 0.3 | `ui/AppShell.kt` adaptive 3-pane shell + `desktopApp/Main.kt` entry point |
| 2026-10-05 | 0.5 | `ir.mahozad.vlc-setup` 0.1.0 pinned, libVLC 3.0.21, `BundledVlc` discovery helper |
| 2026-10-05 | 0.7 | `.github/workflows/build.yml` — Windows MSI+EXE job, Android APK job |
| 2026-10-05 | — | `core`: `CookieStore` + `HttpDownloader` written against the *verified* pinned extractor API |
| 2026-10-05 | — | Android module skeleton (`androidApp`) added but excluded unless `-Pyt.android=on` |
| 2026-10-06 | 0.2 | AGP pinned to **8.7.3** — Kotlin 2.1.21's `KotlinAndroidTarget` still references `BaseVariant`, removed in AGP 8.9 |
| 2026-10-06 | 0.7 | Android SDK bootstrap rewritten (use preinstalled SDK or install cmdline-tools itself); `gradlew` mode set to `100755` |
| 2026-10-06 | 0.7 | Fixed: JitPack 404 on the extractor pin, missing `android.useAndroidX`, libVLC mirror TLS cert |
| 2026-10-06 | 0.8 | `tv`/`phone` flavours; `src/tv/` manifest overlay (`leanback`, `banner`, `touchscreen` not required) + generated 320×180 banner |
| 2026-10-06 | 0.8 | `TvUtils` (ported from NewPipe's `AndroidTvUtils`) + `TvShell` D-pad shell with visible focus ring; landscape + keep-screen-on on TV |
| 2026-10-06 | 0.7/0.8 | ✅ Run `37423534342` **fully green** — `windows-installer`, `android-apk`, `android-tv-release` all uploaded |
| 2026-10-06 | 0.4 | `VlcPlayer` (wraps `EmbeddedMediaPlayerComponent`, mirrors libVLC events into snapshot state) + `VlcVideoSurface` `SwingPanel` host + `VlcPlayerHarness` URL/transport row for spike S3. Compiles locally |
| 2026-10-06 | S1 | ✅ Green locally (12 s). New `spikes` CI job runs `:core:test --tests '*SpikeTest*'`. YouTube returned 20 search results, 5 audio streams, DASH + HLS, and served 1024 bytes over HTTP 206 — **R1 closed** |
| 2026-10-06 | S3 | ✅ `PlaybackSpikeTest` green on the clean Windows runner (run `37437100182`, as an extra step in the windows job): direct video-only 144p URL advanced 2150 ms at 256×144, direct audio itag 139 advanced 2151 ms, local mp4+srt advanced 2402 ms at 480×270 with 2 subtitle tracks — **R3 closed**, R7 → 🟡 monitoring. Found: VLC 3.x cannot claim YouTube manifest URLs (ps-demuxer stall); `appResources` generated output is now git-ignored (not the ~600 staged DLLs); UPX disabled during spikes |
| 2026-10-06 | 1.1 | ✅ **Slice A** landed (`6b58e40`) and is green locally + on CI (run `37442108967`, whole workflow green). `core/service`, `core/model`, `core/player`, `core/errors`; `YoutubeService` drives the extractor on `Dispatchers.IO`, R7 smoke: resolvePlayback picked **vp9 itag 313 (2160p)** + opus itag 251 for the smoke video; kiosks discovered at runtime: `Trending, live, trending_gaming, trending_movies_and_shows, trending_music, trending_podcasts_episodes`; `Phase1ServiceSmokeTest` (search/suggestions/trending/pagination/resolve/video page) added to the `spikes` job |
| 2026-10-06 | 1.9 | ✅ **Update layer** landed (`ui/update`): `UpdatePane` checks GitHub `releases/latest` (lenient regex parser, no new deps), filters assets by platform (Windows .msi/.exe, Android TV `-tv.apk`, phone plain `.apk`), shows what's-new + download/open-releases; `NavigationPane` (`AppScreen`) replaces the Nav placeholder and wires Browse/Search/Updates in the desktop app. Desktop compiles + `UpdateLayerParserTest` green locally |
| 2026-10-06 | 1.3/1.4 | ✅ **Slice B part 1** (UI feed) landed: `ui/feed` — `ResourceFeed` (paging state holder, `PagedResult` continuation chaining) + `FeedColumn`/`VideoCard`/`PageCard` (Coil 3 `AsyncImage`, infinite-scroll via `snapshotFlow`, duration/view formatting); `ui/browse`, `ui/search` — `TrendingScreen` (runtime kiosk chips, feed swap) + `SearchScreen` (250 ms debounced live suggestions, Enter-to-search) wired into the desktop app; clicking a video now sets the pending-play title above the VLC harness. Desktop compiles clean locally |
| 2026-10-06 | 1.6 | ✅ **Slice C engine** landed: `VlcPlayerEngine` implements the core `PlayerEngine` — plays the video-only master with its DASH audio slave attached as libVLC `:input-slave=#audio#` (one clock, S1 finding); `PlayerChrome` (now-playing line, tap-to-seek progress, play/pause/stop/±10s) replaces the harness; `Main.kt` resolves the picked video and starts playback, surfacing resolution failures. S3 gains a fourth gate: `PlaybackSpikeTest` now also plays a video+audio pair and asserts picture, sound, and forward motion — new CI run is the verdict |
| 2026-10-06 | 1.4 | ✅ **Search history** persisted (`SearchHistoryStore`, `user.home/.yt-desktop/search-history.txt`): most-recent-first, deduped, capped at 20, clearable "Recent searches" list that re-submits on click; suggestions now suppressed once a search is submitted. Slice C complete; all-three-jobs CI green (`37451566812`) including the S3 video+audio pair gate |
| 2026-10-06 | 1.5 | ✅ **Channel screen** landed (`ChannelScreen`): header (avatar/subscribers/description), tab chips auto-selecting the first tab, per-tab self-paging feeds, back navigation; channel rows in the Trending/Search feeds now push the channel page via `onChannelClick` (`FeedColumn` gained the callback; shared `SurfaceChip` extracted to `ui/common`) |
| 2026-10-06 | 1.8 | ✅ **Error surfaces** landed (slice D): `ErrorCard` (action label + stable message + retry) powers every feed, the channel screen (with keyed retry), and the player chrome; `ResourceFeed` now carries core `ErrorInfo` (YtException-aware), `reportLoadFailure` takes the taxonomy, `UserAction.UNKNOWN` added as the catch-all. Desktop + core smoke tests green locally |
