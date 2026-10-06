# YT Desktop

A lightweight, ad-free YouTube front-end for **Windows** and **Android**, built with
Kotlin, Compose Multiplatform, and libVLC.

No account. No tracking. No Google Play Services.

> **Status:** active development. See [`plan.md`](plan.md) for build progress.

---

## Features

### Planned (Phase 0–2, in progress)

- Browse trending / kiosk content
- Search with live suggestions and local search history
- Channel pages with all playable tabs
- **Unified in-app player** — plays remote streams *and* your downloaded files with
  the same controls, same window
- Multi-threaded resumable downloads (512 KiB blocks)
- Audio-only downloads (M4A / Opus)
- Subtitle downloads, converted TTML → SRT, auto-loaded from a sidecar file
- Local media library — point it at a folder and play it
- Always-on-top mini player
- Adaptive layout across three window widths
- Global media-key support and tray integration

### Not yet implemented

Playlists, subscriptions, channel groups, feed, history, comments, settings parity,
backup/restore, import/export. See `plan.md` for the full roadmap.

---

## Downloads

| Output | Platform | Status |
|---|---|---|
| `.msi` installer + portable `.exe` | Windows x64 | build in progress |
| `.apk` | Android | build in progress |

Both are produced by GitHub Actions. Grab them from the
[Releases](../../releases) page or the workflow artifacts.

---

## Building from source

### Requirements

- JDK 21
- (Android only) Android SDK 35

### Desktop

```bash
./gradlew :desktopApp:run              # run it
./gradlew :desktopApp:packageMsi      # build the Windows installer
./gradlew :desktopApp:packageDeb      # Linux
./gradlew :desktopApp:packageDmg      # macOS
```

### Android

```bash
./gradlew :androidApp:assembleDebug
```

---

## Architecture

```
core/        pure Kotlin/JVM — no Compose, no Android
ui/          Compose Multiplatform shared UI
desktopApp/  Compose Desktop shell
androidApp/  Android shell
```

`core` is plain JVM bytecode consumed by both apps, so all the logic — service
abstraction, extraction, downloads, muxing, the player engine — is written once.

---

## Project layout

| Module | Contents |
|---|---|
| `core/service` | Pluggable `StreamingService` abstraction (currently YouTube only) |
| `core/downloader` | `NewPipeExtractor` `Downloader` implementation over OkHttp |
| `core/player` | `PlayerEngine` interface + the playback state machine |
| `core/download` | Download engine and post-processing muxers |
| `core/streams` | Pure-JVM MP4/WebM/Ogg/SRT container readers and writers |

---

## Attribution

YT Desktop is **not** affiliated with, endorsed by, or a fork of NewPipe. It is an
independent project inspired by NewPipe's design and built on these GPL-licensed works:

- **[NewPipe](https://github.com/TeamNewPipe/NewPipe)** by NewPipe e.V. — design reference
- **[NewPipeExtractor](https://github.com/TeamNewPipe/NewPipeExtractor)** by TeamNewPipe — service extraction
- **[libVLC](https://www.videolan.org)** by VideoLAN — media playback (LGPL-2.1-or-later)

"NewPipe" is a registered word mark of NewPipe e.V.
See <https://newpipe-ev.de/policy/trademark/>.

---

## License

**GPL-3.0-or-later.** See [`LICENSE`](LICENSE).

Because libVLC is LGPL-2.1-or-later, the libVLC DLLs are shipped in a separate
`libvlc/` folder and may be replaced by the end user.
