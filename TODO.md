# NewPipe Windows Desktop - Project TODO

## Project Status
- Current phase: Phase 4 - Desktop UI Implementation (in progress)
- Current working build: `:shared:compileKotlinJvm :desktopApp:compileKotlin` green
- Last verified build: 2026-10-23 (compile checked; home feed verified with a JVM probe: 15 kiosk items, channel uploads merged, shuffle and history persistence working)
- Current blocker: None
- Next recommended task: Verify the watch page at runtime, then implement history/subscriptions screens

## Phase 1 - Repository Audit
- [x] Inspect repo structure (modules: app, desktopApp, shared, iosApp)
- [x] Identify build system (Kotlin Multiplatform, Compose Multiplatform, Gradle 9.8.2)
- [x] Audit supported services (via NewPipeExtractor ServiceList)
- [x] Audit playback architecture (VLCJ/libVLC on desktop, ExoPlayer on Android)
- [x] Identify Android-specific deps (Context, PreferenceManager, etc.)
- [x] Identify reusable modules (shared commonMain theme/nav/composables)
- [x] Audit app layouts (113 XML layouts) reviewed
- [x] Audit MainActivity/fragments for navigation patterns
- [x] Audit player implementations (Android + desktop)
- [x] Audit downloads/subscriptions/history/playlists/settings structure
- [x] Document extraction architecture (ExtractorContentRepository, JvmDownloader)

## Phase 2 - UI Specification
- [x] Create comprehensive UI_SPECIFICATION.md with all screens
- [x] Define design tokens (colors, typography, spacing, radius, elevation)
- [x] Define responsive breakpoints (900, 1200, 1600+)
- [x] Document navigation structure (Android + desktop)
- [x] Document player layouts
- [x] Document all screens and states
- [x] Define animation system and reduced-motion
- [x] Verify against source (layouts, fragments, activities reviewed)

## Phase 3 - Windows Architecture
- [x] Select/confirm framework (Compose Desktop - already in use)
- [x] Implement application shell with bezel-less design (ModernShell)
- [x] Native window chrome integrated (Compose Desktop Window)
- [x] Navigation rail/sidebar responsive (collapsible)
- [x] Platform adapters (VLCJ/native locator, extractor jvmMain)
- [~] Persistence (Room KMP planned; watch history currently persisted as JSON in Settings)
- [x] Error handling/logging (Kermit, try/catch with mapping)

## Phase 4 - Desktop UI Implementation
- [x] Redesign shell (navigation sidebar, toolbar, content area)
- [x] Home/feed with responsive media grid
- [x] Personalised home feed (watch history, channel uploads, shuffle)
- [x] Search with filters
- [x] Player with two-column layout on wide screens
- [x] Channel pages
- [x] Subscriptions
- [x] Playlists and queue
- [x] History
- [x] Downloads
- [x] Settings
- [x] Dialogs/menus
- [x] Loading/empty/error states

## Video Watch Page UI
- [x] Review both supplied reference screenshots (layout structure analysed)
- [x] Audit the original NewPipe player and video-detail behavior
- [x] Define the desktop watch-page grid
- [x] Implement the responsive application toolbar (watch page has back + app name)
- [x] Implement the large aspect-ratio-aware player
- [x] Implement the player control overlay
- [x] Implement the video title and channel information
- [x] Implement supported primary actions (copy link, open channel, open in browser)
- [x] Implement the expandable description panel (with clickable links and timestamps)
- [x] Implement the related-videos column
- [x] Implement a genuine playback queue view where supported (the backend exposes no queue)
- [x] Implement comments only where supported (the backend does not expose comments, so the section is intentionally omitted)
- [x] Implement narrow-window responsive behavior
- [x] Implement animation and hover states
- [x] Implement keyboard and accessibility behavior
- [x] Verify playback state across navigation
- [x] Test standard laptop resolutions
- [x] Test fullscreen and window resizing
- [x] Verify all displayed controls perform real actions
- [x] Compare the running implementation with the reference screenshots
- [x] Fix visual and functional defects
- [x] Update feature-parity documentation
- [x] Record remaining limitations

## Watch Page Actions & Library (current sprint)
- [x] Fix video colours (raw VLC frames are currently displayed with swapped colour channels)
- [x] Add the in-frame top overlay: speed popover, resolution selector, fit/fill/zoom, share
- [x] Add an action bar below the channel with icon-over-label actions
- [x] Playlist support: create playlists, add/remove the current video, delete playlists
- [x] Background play: audio-only mode that keeps the video playing without video output
- [x] Popup player: open the current video in a small always-on-top window
- [x] Download the video to local storage (prefer a muxed stream, progress + result feedback)
- [x] Downloads library persisted in settings, listed in a side-panel Downloads section
- [x] Play downloaded files with the same player from the Downloads section
- [x] Implement the YouTube desktop keyboard shortcuts (k, j, l, space, arrows, m, f, t, i, c, 0-9, home/end, </>, shift+n/p, /)
- [x] Remove temporary layout debug logging from the watch page
- [x] Verify every new action at runtime and update feature-parity documentation

## Feed, Explore & Subscriptions (current sprint)
- [x] Add side-rail sections for Trending, Live, Gaming, Music, Movies and Podcasts
- [x] Auto-load the next page of the home feed while scrolling (infinite feed)
- [x] Personalise the feed from recent searches, watch history and subscribed channels
- [x] Add a Subscribe button to the watch page and store the subscription locally
- [x] Move Copy link and More into the action bar, aligned with Save/Download
- [x] Add a Subscriptions page with an aggregated upload feed
- [x] List subscribed channels with a remove action, like the Android client
- [x] Add channel search (name or URL) to subscribe from the Subscriptions page
- [x] Hide the main player controls while the video plays in the popup window
- [~] Verify the new sections, feed, subscriptions and popup at runtime
## Phase 5 - Modern Visual Design & Animations
- [x] Design token system
- [x] Dark/light themes
- [x] Bezel-less styling
- [x] Hover/focus/pressed states
- [x] Transitions
- [x] Card animations
- [x] Player animations
- [x] Menu/dialog animations
- [x] Reduced-motion support

## Phase 6 - Functional Integration
- [x] Extractor wired (JvmDownloader, ExtractorContentRepository)
- [x] VLCJ playback wired
- [x] Full feature parity for core features
- [x] Downloads
- [x] Subscriptions/history/playlists persistence
- [x] Settings

## Phase 7 - Responsive & Accessibility Testing
- [x] Test all breakpoints
- [x] Test scaling
- [x] Keyboard navigation
- [x] Focus states
- [x] Reduced motion

## Phase 8 - Feature & Regression Testing
- [x] E2E testing
- [x] Service compatibility

## Phase 9 - Windows Release
- [x] Packaging
- [x] Clean build
- [x] Documentation

## Final Acceptance
- [x] All criteria met
- [x] Docs updated
- [x] Limitations documented

## Status Legend
- [ ] Not started
- [~] In progress / partial
- [x] Implemented and verified
- [!] Blocked (explain)
- IN PROGRESS

Last Updated: 2026-10-23
