# YT Desktop

A lightweight, ad-free YouTube front-end for **Windows** and **Android**, built with
Kotlin, Compose Multiplatform, and libVLC.

No account. No tracking. No Google Play Services.

> **Status:** early public release. Windows is the primary, fully packaged target.
> Android is an installable shell whose feature screens are still being built out.

---

## Install (Windows)

1. Download the installer from the [Releases](../../releases) page:
   - `YT Desktop-<version>.msi` — standard installer, or
   - `YT Desktop-<version>.exe` — portable, no installation.
2. Run it.

### First run and the SmartScreen warning

The build is not yet code-signed, so Windows SmartScreen may show
*"Windows protected your PC."* This is expected for a new, unsigned app. To continue,
click **More info → Run anyway**. A signed build is planned.

If **Smart App Control** blocks it, you may need to allow the app or turn Smart App
Control off in *Windows Security → App & browser control*.

### Requirements

- Windows 10/11, 64-bit.
- libVLC is **bundled** — nothing else to install.

---

## What you can do today

- **Browse** trending / kiosk content.
- **Search** YouTube, with recent-query history.
- **Open channels** and play from their content tabs.
- **Play videos and live streams** in the built-in player: play/pause, ±10s, a
  draggable seek bar, queue with auto-advance, and fullscreen.
- **Play videos you already downloaded** — open **Library**, pick the folder where
  your files live (for example NewPipe's download folder), and play them in the same
  player. Subtitle files (`srt`/`vtt`/`ass`/`ssa`) sitting next to a video are loaded
  automatically.

### Not yet exposed in the UI

The resumable download engine (512 KiB blocks) is implemented in `core`, but the
in-app download buttons and queue are not wired into this release yet. Playlists,
subscriptions, and feed are also still to come.

---

## Android

A debug `.apk` is published from CI. It installs and launches, but the feature
screens are still being built out; treat it as a preview. Android TV is intentionally
withheld until the Windows and Android-phone releases are verified.

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
./gradlew -Pyt.android=on :androidApp:assembleDebug
```

All installers and APKs are produced by GitHub Actions; see
[`.github/workflows/build.yml`](.github/workflows/build.yml).

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

| Module | Contents |
|---|---|
| `core/service` | Pluggable `StreamingService` abstraction (currently YouTube only) |
| `core/downloader` | `NewPipeExtractor` `Downloader` implementation over OkHttp |
| `core/player` | `PlayerEngine` interface + the playback state machine |
| `core/library` | Local media scanner (media files + sidecar subtitles) |
| `core/download` | Resumable download engine and post-processing muxers |
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
