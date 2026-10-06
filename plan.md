# YT Desktop — Build Plan

> **Status:** Phase 0 — in progress
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
| GitHub CLI `gh` | ⚠️ installed, not authenticated | User must run `gh auth login` |
| Disk headroom | ⚠️ 13.8 GB free | Hard checkpoint: purge caches if < 5 GB |
| Android SDK | ⏳ only needed for the APK build | CI installs it; local desktop build does not need it |

---

## Phase 0 — Toolchain + Spikes (~400 lines)

- [x] **0.1** Install JDK 21, verify `java -version`
- [x] **0.2** Scaffold Gradle project — settings, root build, version catalog, wrapper 8.13
- [x] **0.3** Compose Desktop shell, adaptive layout (3 pane widths) — *written, never compiled*
- [ ] **0.4** VLCJ 4.11.0 + Direct Rendering video surface — dependency wired, surface pending
- [x] **0.5** `ir.mahozad.vlc-setup` + bundle libVLC into the installer — *written, never executed*
- [ ] **0.6** `packageMsi` → `YTDesktop-0.1.0.msi` — config written, task never run
- [x] **0.7** GitHub Actions workflow for MSI **and** APK — *written, needs first CI run*

> **Verification status:** no Gradle task has completed successfully yet. On the dev
> machine `dl.google.com` is unreachable and Maven Central is very slow, so the local
> network cannot resolve the Kotlin compiler jar. **GitHub Actions is the verification
> path** for compilation, packaging and the APK — treat every unchecked/asterisked item
> above as unproven until a green CI run exists.

### Spikes — ALL must pass before Phase 1 starts

- [ ] **S1** YouTube search + stream resolution from a plain JVM (no WebView)
- [ ] **S2** PoToken via `bgutil-pot.exe` subprocess (cookies **OFF**)
- [ ] **S3** libVLC Direct Rendering: remote DASH + local `.mp4` + `.srt` in one window

---

## Phase 1 — Browse / Search / Play (~7K lines)

- [ ] **1.1** Service layer — pluggable `StreamingService` interface, YouTube impl
- [ ] **1.2** OkHttp `Downloader` + Brotli + cookie jar
- [ ] **1.3** Trending kiosks (8 YouTube kiosk IDs)
- [ ] **1.4** Search + suggestions + search history
- [ ] **1.5** Channel page (7 playable tabs)
- [ ] **1.6** Unified player — `PlaybackSource` seam (remote vs local file)
- [ ] **1.7** Play queue (4 pagination strategies)
- [ ] **1.8** Error taxonomy (`ErrorInfo` / `UserAction` parity)

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

---

## Risk Register

| ID | Risk | Status | Mitigation |
|---|---|---|---|
| **R1** | YouTube extraction from a plain JVM | 🔴 open | Gated by spike **S1** |
| **R2** | PoToken requirement | 🔴 open | Solution known — gated by spike **S2** |
| **R3** | libVLC DASH/HLS + subtitle rendering | 🔴 open | Gated by spike **S3** |
| **R4** | YouTube OTF / post-live DVR manifest synthesis has no libVLC equivalent | 🟡 accepted | Feed libVLC direct progressive/HLS URLs; accept fidelity gap |
| **R5** | 13.8 GB disk headroom | 🟡 monitoring | Hard checkpoint: purge Gradle caches below 5 GB |
| **R6** | **YouTube-only means no fallback service.** If YouTube breaks, there is nothing else to show | 🟡 accepted | Keep `StreamingService` pluggable so recovery is cheap |
| **R7** | **libVLC 3.x cannot decode AV1.** YouTube defaults to AV1/VP9; VLC 3.0.21 covers VP8/VP9/H.264/H.265 + audio but *not* AV1 or H.266 | 🔴 open | Resolve AV1-explicit streams to a VP9/H.264 variant before handing the URL to VLC. Needs a spike to confirm which formats fail in practice |
| **R8** | No local build possible — `dl.google.com` unreachable, Maven Central crawl rate. Nothing has been compiled | 🔴 open | Verify via GitHub Actions on every push; do not trust "written" code until CI is green |
| **R9** | Extractor API drift: at the pinned commit `Downloader.execute(Request)` is the only abstract member (`getAsStream`/`getContentLength` were removed upstream) | 🟢 handled | `HttpDownloader` implements only `execute`; re-check on every extractor bump |

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
