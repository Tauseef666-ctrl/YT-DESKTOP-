# YT Desktop 0.2.0

A lightweight, ad-free YouTube front-end for Windows. No account, no tracking,
no Google Play Services.

## Highlights

- **Built-in player** — play YouTube videos and live streams with play/pause, ±10s,
  a draggable seek bar with running time, a queue that auto-advances, and a
  fullscreen toggle.
- **Play your own downloads** — the new **Library** lets you point the app at a
  folder of videos you already downloaded (for example NewPipe's download folder)
  and play them in the same player. Subtitle files (`srt`/`vtt`/`ass`/`ssa`) next to
  a video are picked up automatically.
- **Browse & search** — trending/kiosk content, search with recent-query history,
  and channel pages.

## Downloads

| File | What it is |
|---|---|
| `YT Desktop-<version>.msi` | Windows installer |
| `YT Desktop-<version>.exe` | Portable, no install |
| `.apk` | Android preview shell (installs and launches; screens still in progress) |

## Installing on Windows

1. Download the `.msi` (or the portable `.exe`).
2. Run it. Windows SmartScreen may warn because the build is not yet code-signed —
   choose **More info → Run anyway**. If Smart App Control blocks it, allow the app
   or turn Smart App Control off in *Windows Security → App & browser control*.

libVLC is bundled; nothing else to install. Windows 10/11 64-bit required.

## Notes

- The resumable download engine is implemented in `core` but its in-app buttons and
  queue are not exposed in this release yet.
- Live streams play as a single manifest; anonymous live playback may not work from
  every network.
- "NewPipe" is a registered word mark of NewPipe e.V.; YT Desktop is an independent
  project, not affiliated with or endorsed by NewPipe.
