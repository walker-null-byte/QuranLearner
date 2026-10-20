# 📖 Noor — Quran Learning App
## Comprehensive Development Plan (PLAN.md)
**Platform:** Android, iOS & Desktop (Kotlin Multiplatform + Compose Multiplatform)  
**Date:** May 2026  
**Version:** 1.1.0 — Improved MVP Plan

**Note:** This file is updated when features land or structure changes; ask for a refresh after completing a milestone if you want the checklist and layout kept in sync.

---

## Current Status (Read Me First)

- **Android** — primary target and fully working: reader, audio, offline downloads, export via SAF, reminders.
- **Desktop (JVM)** — working: database seeds/self-repairs automatically, real audio playback, offline downloads, and export.
- **iOS** — entry points fixed (removed a stray Koin call, DB seeding with fallbacks); the Kotlin/Native sources cross-compile on Linux, but the app is **not built/run on device or simulator** (requires macOS/Xcode).
- **Web / Wasm** — **removed from the Gradle build**; deferred future work (no SQLite driver exists for Wasm).
- **Cloud sync / accounts** — implemented: **Google sign-in + Supabase backup** (own project). Schema in [`supabase/schema.sql`](supabase/schema.sql), setup guide in [`supabase/README.md`](supabase/README.md). Builds without `supabase.*` keys in `local.properties` run local-only, and the sync path has not yet been exercised against a live Supabase project.
- **QA / store submission** — not done.

### Known Gaps
- Web/Wasm target (deferred — `androidx.sqlite` has no Wasm build).
- Live verification of cloud sync (schema, RLS and the sync engine are in place; needs a real Supabase project + Google OAuth credentials to test end-to-end).
- Tajweed highlighting (needs annotated text data).
- On-device iOS verification (needs macOS/Xcode).
- `compileCommonMainKotlinMetadata` (the IDE metadata task) fails because `commonMain` references `androidx.sqlite`, which publishes no common metadata. Per-target compilations (Android / Desktop / iOS) are unaffected and fully green.
- Play Store submission and the QA checklist.

---

## 1. Research Findings

### 1.1 Indo-Pak Script (Naskh Nastaleeq)
The Indo-Pak script is the South Asian variant of Quranic text written in Naskh-Nastaleeq style, used across Pakistan, India, and Bangladesh. It differs from Uthmani in glyph shapes, pause marks, and use of Urdu-style Arabic characters.

**Sources & Fonts:**
- **Quranic Universal Library (QUL):** `https://qul.tarteel.ai/resources/quran-script/59` — actively maintained IndoPak Hanafi text and font files
- **QuranWBW / Quran.com:** Refined version with accurate pause marks and rounded ayah icons
- **PakType:** Unicode-based Pakistani Mushaf with proper Pakistani Quranic marks (U+08D4 to U+08E2)
- **Font:** `AlQuran IndoPak by QuranWBW` (TTF/WOFF2) or `Indopak Nastaleeq` from QUL

**Technical Notes:**
- Uses Arabic Tah Marbuta (U+0629), Kaf (U+0643), Heh (U+0647), Yeh (U+064A) instead of Urdu variants
- End-of-ayah uses U+06DD with Arabic numbers + Pakistani Quranic marks
- Requires specialized ligature support for proper rendering

### 1.2 Uthmani Script (Madinah Mushaf)
The Uthmani script is the standard Middle Eastern/North African mushaf style, used by the King Fahd Glorious Quran Printing Complex (KFGQPC). It is the most widely recognized digital Quranic script.

**Sources & Fonts:**
- **KFGQPC Fonts:** `Uthmanic Hafs` and `Uthman Taha Naskh` — freely available, non-commercial license from King Fahd Quran Printing Complex
- **QUL Uthmani:** JSON API with verse_key, text, page_number, juz_number metadata
- **Uthmani Mushaf GitHub:** Unicode-based HTML5 files with proper Open Tanween codes (U+08F0, U+08F1, U+08F2)
- **Font:** `KFGQPC HAFS Uthmanic Script` (TTF) or `me_quran` for digital rendering

**Technical Notes:**
- Uses Farsi Yeh (dotless final shape) and Alef Maqsura where needed
- Proper usage of U+0675 (High Hamza Alef)
- End-of-ayah with Arabic numbers
- Each ayah has juz, manzil, hizb, and hizb quarter attributes

### 1.3 Open Source / Royalty-Free Recitation (Ayah-by-Ayah)
Quran recitation itself is not copyrightable as divine text, but *recordings* may have publisher rights. For a completely safe, royalty-free approach:

**Recommended Sources:**
- **Quran.com Audio API (quranjs.com):** Verse-by-verse audio with multiple reciters. Provides direct MP3 URLs per ayah with timing segments
  - Reciters: Mishary Rashid Alafasy (id: 7), Abu Bakr Al-Shatri, etc.
- **EveryAyah.com:** Historical source for segmented MP3s. Organized as `/[reciter]/[surah]/[ayah].mp3`

**Implementation Strategy:**
- Stream audio from CDN (do not bundle 6000+ MP3s)
- Cache recently played ayahs locally (LRU disk cache ~100MB) using platform-specific media caching.
- Include attribution: "Audio courtesy of Quran.com" or respective reciter
- Allow offline download per-surah or per-juz (user-initiated Download Manager)

### 1.4 Open Source Translations & Transliterations

**Translations (Verified Open/Permissive):**
 Translation | License | Notes |
-------------|---------|-------|
 **Yusuf Ali** (1934) | Public Domain | Classic, widely trusted |
 **Pickthall** | Public Domain | Literal style, older English |
 **Dr. Mustafa Khattab (The Clear Quran)** | Creative Commons | Modern English, very readable |
 **Mufti Taqi Usmani** | Free for non-commercial use | Hanafi scholarly translation |

**Transliteration:**
- Use **QUL / Tanzil** transliteration data (Latin script representation)
- Store as separate field in local DB

---

## 2. Tech Stack (Updated 2026)

| Layer | Technology | Version | Reason |
|-------|-----------|---------|--------|
| **Language** | Kotlin | 2.3.21 | Current stable project version |
| **UI Framework** | Compose Multiplatform | 1.10.3 | Stable with deep platform integration |
| **Material Design** | Material3 | 1.10.0-alpha05 | Adaptive color, dynamic theming |
| **Architecture** | MVVM (screen ViewModels) | — | Clear separation per screen |
| **DI** | `CompositionLocal` + `viewModel { }` factories | — | No DI framework; `LocalQuranDb` provides DB |
| **Database** | androidx.sqlite + `BundledSQLiteDriver` | — | `QuranDb` with explicit SQL (no Room in this module) |
| **SQLite Driver** | BundledSQLiteDriver | — | Per-platform dependency in `build.gradle.kts` |
| **Networking** | Ktor Client | 3.4.3 | supabase-kt's HTTP stack; engines per platform (OkHttp / CIO / Darwin) |
| **Cloud backend** | Supabase (Postgres + Auth) via supabase-kt | 3.6.0 | Google sign-in + per-user backup tables with RLS; schema in `supabase/` |
| **Serialization** | Kotlinx Serialization | 1.11.0 | JSON without reflection |
| **Images / assets** | Compose `composeResources` | — | Fonts & drawables bundled with CMP |
| **Navigation** | Navigation Compose KMP | 2.9.2 | Type-safe, KMP-native navigation |
| **Preferences** | SQLite `settings` table (`QuranDb`) | — | Key/value flags (e.g. onboarding); DataStore optional later |
| **Audio (Android)** | ExoPlayer (Media3) | 1.5+ | Robust ayah-by-ayah playback |
| **Audio (iOS)** | AVPlayer | — | Native iOS audio (expect/actual wrapper) |
| **Audio (Desktop)** | javax.sound + mp3spi | mp3spi 1.9.5.4 | Real JVM playback (MP3 via mp3spi) |
| **Build** | Gradle | 9.4.1 | Current project version |

---

## 3. Architecture & Directory Structure

Single Gradle module **`composeApp`** (see `settings.gradle.kts`). Shared code lives under **`commonMain`**; platform code under **`androidMain`**, **`iosMain`**, and **`desktopMain`**. (A Web/Wasm source set was removed — see Known Gaps.)

```
QuranLearner/
├── PLAN.md
├── composeApp/
│   └── src/
│       ├── commonMain/
│       │   ├── kotlin/com/dillu/quranlearner/
│       │   │   ├── App.kt                 # Compose entry, navigation scaffold
│       │   │   ├── Platform.kt            # expect/actual platform surface
│       │   │   ├── db/                    # QuranDb: SQLite (androidx.sqlite) + models & queries
│       │   │   └── ui/
│       │   │       ├── navigation/      # Route / destination types
│       │   │       ├── components/      # Shared composables (e.g. expect AudioPlayer)
│       │   │       ├── screens/         # Feature screens + their ViewModels
│       │   │       └── theme/           # Material3, typography, palette
│       │   └── composeResources/        # CMP resources (fonts, drawables, etc.)
│       │       ├── drawable/
│       │       └── font/
│       ├── commonTest/kotlin/           # Shared unit tests (when added)
│       ├── androidMain/                 # MainActivity, manifest, res, ExoPlayer + downloader actuals
│       ├── iosMain/                     # MainViewController, AVPlayer actual
│       └── desktopMain/                 # JVM entry + javax.sound audio, downloader, exporter actuals
```

**Target layout (not present yet):** A larger refactor might introduce `di/`, `data/` (local + remote), and `domain/` as in earlier sketches; today persistence and queries are concentrated under **`db/`**, and feature logic largely lives in **`ui/screens/*ViewModel`**.

### 3.1 Data Flow & Offline Strategy
- **Pre-population:** On first launch, platforms copy or open a bundled **`pre_populated_quran.db`** (e.g. Android copies from **`assets/`** into app files, then opens with the bundled SQLite driver). iOS/desktop use their own bundle/classpath paths as wired in platform entry code.
- **Offline-First:** Quran text is read from the local database after that first setup.
- **Audio:** Streamed by URL from the reader. Per-surah offline download is implemented on Android (ExoPlayer cache) and Desktop (`~/.quranlearner/audio`); per-juz download remains future work.

---

## 4. Feature Specification (Refined)

### 4.1 Onboarding & Reader
- **Script Selection:** Toggle between **Indo-Pak** and **Uthmani** with live preview.
- **Learning Speed:** Presets for daily ayahs with estimated completion timeline.
- **Focus Mode:** Minimalist reading interface that hides all UI chrome during reading.
- **Tajweed Highlights:** Color-coded text for Uthmani script.

### 4.2 Home & Progress
- **Streak Card:** Current streak with animations.
- **Calendar Heatmap:** GitHub-style activity grid.
- **Achievements:** Milestone badges (🌱 First Step, 🔥 7-Day Streak, etc.).

### 4.3 Settings & Customization
- **Download Manager:** Manage offline audio storage.
- **Font Size Sliders:** Independent controls for Arabic and translation text.
- **Reciter Selection:** Switch between various world-renowned reciters.

---

## 5. Build Phases

### Phase 1: Foundation (Weeks 1–3)
- [x] KMP project setup (`composeApp` module): Compose Multiplatform, androidx.sqlite + bundled driver, Navigation Compose.
- [x] Seed database with Quran text via bundled `pre_populated_quran.db` copied/opened on first launch (platform entry points).
- [x] Material3 Theme setup (Deep Navy, Emerald, Amber, Sage) with Glassmorphism aesthetics.
- [x] Typography setup using Inter (UI) and Amiri Quran (Arabic).
- [x] Navigation scaffold with bottom bar.

### Phase 2: Core Reader (Weeks 4–6)
- [x] Surah list and Reader screen implementation (Clean Ayah cards, inline bookmarks).
- [x] Script and Translation toggles with regex HTML tag stripping.
- [x] Audio streaming implementation (expect/actual) tracking progress, currentMs, and durationMs.
- [x] Docked bottom audio player with squiggly progress bar, loop toggle, and auto-advance.

### Phase 3: Learning Engine (Weeks 7–9)
- [x] "Mark as Learned" logic and streak engine.
- [x] Achievement system — 17 achievements with full trigger logic in `ReaderViewModel` (milestones, streaks, time/date, surah completion, Juz Amma, marathon, 3 Quls).
- [x] XP & leveling system with daily progress tracking.
- [x] Stats/Profile screen with activity heatmap, achievement grid, privacy dialog, daily goal editor.
- [x] Local push notifications for daily reminders — expect/actual `NotificationScheduler` with Android AlarmManager, iOS UNUserNotificationCenter, desktop no-op stub. Reminder toggle + time picker UI in Settings screen.

### Phase 4: Polish & Launch (Weeks 10–14)
- [x] 2-Page Onboarding flow with AnimatedContent, Supabase Auth placeholders, and Glassmorphism.
- [x] Review screen SRS button color consistency fix (glassmorphism-style bordered buttons).
- [x] Stats screen visual refactor — fully migrated from `MaterialTheme.colorScheme` to `NoorColors` (glassmorphism cards, bordered badges, custom progress bar, styled dialogs).
- [x] Search — search bar in `SurahListScreen` with filtering by name/number/translation + animated clear button + empty state. Also migrated SurahListScreen to NoorColors.
- [x] Favorites — heart toggle on `AyahCard`, `favorites` table in DB, `FavoritesScreen` with remove action and empty state.
- [x] Navigation restructure — `ProfileHubScreen` with links to Stats, Settings, Favorites. Separated concerns into dedicated screens.
- [x] Reciter selection — 8 reciters from EveryAyah.com. `SettingsScreen` with radio buttons, persisted to DB, dynamic audio URL builder.
- [x] Font size sliders — Arabic (24–56sp) and translation (12–28sp) sliders in Settings, persisted to DB.
- [ ] QA, performance optimization, and store submission.

---

## 6. Monetization & Ethics
- **Free & Ad-Free:** 100% free and ad-free to maintain sanctity.
- **Privacy:** Local-only data by default. No tracking without consent.
- **Open Source:** MIT/Apache licensed.

---

## 7. Future Plans & Backlog

### High Priority
- [x] **Stats Screen Visual Overhaul:** Fully migrated `StatsScreen` to `NoorColors`. Glassmorphism cards, bordered achievement badges, styled level progress bar, custom heatmap circles, styled dialogs.
- [x] **Local Push Notifications:** `expect/actual` `NotificationScheduler` — Android (AlarmManager + BroadcastReceiver + NotificationCompat), iOS (UNUserNotificationCenter), Desktop (no-op stub). Reminder toggle + quick-select time picker in Settings. Settings persisted in DB.
- [x] **Search:** Search bar in `SurahListScreen` with `filteredSurahs` Flow in ViewModel. Filters by English name, Arabic name, translation meaning, or surah number. Animated clear button, empty-state messaging. Also migrated entire SurahListScreen to NoorColors.

### Medium Priority
- [x] **Favorites / Bookmarks:** Heart toggle on `AyahCard` in the Reader. Separate `FavoritesScreen` accessible from Profile hub. DB `favorites` table with toggle/query/count methods.
- [x] **Reciter Selection:** `SettingsScreen` with 8 reciters (Alafasy, Husary, Minshawi, Abdulbasit, Sudais, Shuraim, Ajamy, Maher). Persisted `reciter_id` in DB. Dynamic `playAyahAudio()` using reciter folder. Dock subtitle shows selected reciter name.
- [x] **Font Size Sliders:** Arabic (24–56sp) and translation (12–28sp) sliders in `SettingsScreen`. Values persisted via `SettingsViewModel`.
- [x] **Download Manager:** Per-surah offline audio download — Android (ExoPlayer cache) and Desktop (`~/.quranlearner/audio`), with export via SAF / directory chooser. Per-juz download remains future work.

### Low Priority / Future
- [x] **Google Sign-In & Cloud Sync (implemented):** Supabase Auth with Google (native Credential Manager picker on Android, browser OAuth on Desktop/iOS), six per-user tables with RLS and a `server_updated_at` trigger (`supabase/schema.sql`), pull-then-push last-write-wins sync engine (`cloud/SyncManager.kt`), tombstoned favorites, Settings account card (Sign in / Sync now / Sign out) and background sync at app start. Pending: end-to-end verification against a live Supabase project (see `supabase/README.md`).
- [ ] **Tajweed Highlights:** Color-coded text for Uthmani script (requires annotated text data or Tajweed API).
- [ ] **Focus Mode Refinement:** Gesture-based controls, swipe to advance ayahs.
- [ ] **iOS-specific polish (unverified — needs macOS/Xcode):** AVPlayer audio wrapper testing, platform-specific notification permissions, on-device build verification.

---

## 8. Session Notes

_This section tracks work done across coding sessions for continuity._

### 2026-05-16
- Reviewed full codebase status against PLAN.md.
- Confirmed achievement system is fully implemented (was previously unchecked).
- Fixed Review screen SRS buttons (Forgot/Hard/Easy) — colors were inconsistent. Now uses glassmorphism-style: transparent backgrounds with 12% alpha, matching NoorColors (Error/Secondary/Primary), subtle borders, consistent 12dp rounded corners.
- **Stats Screen Visual Overhaul:** Rewrote `StatsScreen.kt` to use `NoorColors` + `LocalNoorTypography` everywhere. Glassmorphism cards with `BorderStroke`, styled achievement badges, custom heatmap with `Surface` circles, styled dialogs.
- **Search in SurahListScreen:** Added `searchQuery` + `filteredSurahs` Flow to `SurahListViewModel`. Rewrote `SurahListScreen.kt` with `OutlinedTextField` search bar, animated clear button, empty-state messaging, and full NoorColors migration.
- **Local Push Notifications:** Created `expect class NotificationScheduler` in `commonMain` with `scheduleDailyReminder()` and `cancelDailyReminder()`. Android actual uses `AlarmManager` + `BroadcastReceiver` + `NotificationCompat`. iOS actual uses `UNUserNotificationCenter`. Desktop/Wasm are no-op stubs. Registered receiver in `AndroidManifest.xml`. Added `POST_NOTIFICATIONS` and `SCHEDULE_EXACT_ALARM` permissions.
- Updated PLAN.md with accurate status, future plans backlog, and session notes section.

### 2026-05-16 (Session 2)
- **Features Added:** Favorites/Bookmarks, Reciter Selection (8 reciters), Font Size Sliders, Download Manager placeholder.
- **Favorites / Bookmarks:** DB `favorites` table with `toggleFavorite`, `getAllFavoriteAyahs`, `getFavoriteCount`. Heart icon on `AyahCard` in Reader. `FavoritesViewModel` with load/remove.
- **Reciter Selection:** `ReciterOption` data class + `AVAILABLE_RECITERS` list. `SettingsViewModel` persists `reciter_id`. `ReaderViewModel` loads reciter on init. Dynamic `playAyahAudio()` with `reciterFolder` param. Dock subtitle shows selected reciter.
- **Font Size Sliders:** Arabic (24–56sp) and translation (12–28sp) in Settings. Persisted via `SettingsViewModel`.

### 2026-05-16 (Session 3 — UX Restructure)
- **Eliminated ProfileHubScreen:** The hub-with-cards pattern added unnecessary taps. Replaced with direct tabs.
- **New 4-tab bottom navigation:** `Quran | Review | Stats | Settings`
  - Themed `NavigationBar` with NoorColors (emerald indicator, proper selection tinting).
- **Favorites moved into Quran tab:** `FilterChip` toggle ("All Surahs" / "♥ Favorites") at the top of `SurahListScreen`. Inline favorite cards with Arabic text, translation, surah name, and remove button. Empty state when no favorites.
- **Settings is now a direct tab:** Sign-in card at top (avatar + "Guest Learner" + Cloud Sync button with snackbar). All settings inline below: reciter, font size, reminders, downloads, privacy.
- **Stats is now a direct tab:** No more back button — level/XP, heatmap, daily goal, achievements grid all directly accessible.
- **Deleted files:** `ProfileHubScreen.kt` (no longer needed), standalone `FavoritesScreen` composable (UI moved inline to SurahListScreen, `FavoritesViewModel` kept).
- **Cleaned up navigation:** Removed `Settings` and `Favorites` destinations (no longer separate routes). Only `Onboarding`, `MainApp`, `Reader`, `Stats`, `SurahList` remain.
- **Notification Improvements:** Upgraded Android `AlarmManager` from `setRepeating` to `setExactAndAllowWhileIdle` for precise triggering. Added `USE_EXACT_ALARM` permission to AndroidManifest. Added intent to open `MainActivity` when the notification is tapped.
- **Audio Caching:** Configured ExoPlayer in `AudioPlayer.android.kt` with a `CacheDataSource` and a 100MB `SimpleCache`. Audio is now seamlessly cached to disk as it streams.
- **Data Saver Audio:** Added a toggle in Settings to switch from 128kbps audio to 64kbps audio. `ReaderViewModel` and `FavoritesViewModel` now dynamically replace `_128kbps` with `_64kbps` in the audio URLs if enabled.
- **Continuous Playback (Play Whole Surah):** 
  - Added an `onCompletion` callback to the shared `AudioPlayer` interface and implemented it via ExoPlayer's `Player.Listener` in Android and `NSNotificationCenter` in iOS.
  - Refactored `ReaderScreen` to use the `onCompletion` callback to automatically and seamlessly advance to the next ayah.
  - Added a "Play Whole Surah" icon to the top right of `ReaderScreen`.
  - Added a Quick Play icon to each Surah card in `SurahListScreen`, which navigates to `ReaderScreen` with `autoPlay=true`.
  - Updated `SurahPlayerScreen` to stream the complete Surah as a single `.mp3` file from `mp3quran.net` instead of queuing individual Ayahs. Mapped 8 supported reciters to their respective `mp3quran.net` server directories.
  - Implemented `seekTo` in the cross-platform `AudioPlayer` interface and native platforms (`ExoPlayer.seekTo` on Android, `CMTimeMakeWithSeconds` on iOS) to enable full timeline scrubbing.
  - Replaced the Previous/Next Ayah buttons in the player with 15-second Fast-Rewind and Fast-Forward controls.
- **Offline Downloading Engine:**
  - Built a cross-platform `NoorDownloader` interface with an Android-specific implementation using ExoPlayer's `CacheWriter`.
  - Downloading a Surah now seamlessly pulls down both the full Surah `.mp3` (for the music player) and all individual Ayah slices (for the `ReaderScreen`) directly into the shared `SimpleCache`. This makes them instantly accessible for offline playback using existing logic without changing the `AudioPlayer` data source.
  - Added a "Download" button to the `SurahListScreen` long-press menu, tracking state in `SurahListViewModel` and persisting downloaded status to SQLite via `Settings`.
- Build verified: compiles successfully on Android debug target.

### 2026-10-15 (Documentation & Polish Pass)
- **README.md / PLAN.md** corrected to stop overclaiming and match the real state.
- **Web/Wasm removed** from the Gradle build entirely (`androidx.sqlite` has no Wasm driver); Web is deferred future work, not a supported platform.
- **Desktop (JVM)** confirmed as a real target: DB seeds/self-repairs, audio playback, per-surah offline downloads, and export.
- **iOS** entry points fixed (stray Koin call removed, DB seeding with fallbacks), but still **not build-verified** (needs macOS/Xcode).
- **Cloud sync / accounts:** confirmed **not implemented** — local SQLite only; Settings shows a "Cloud sync coming soon!" snackbar.
- **Dead-code cleanup:** removed the unreferenced `SurahList` and `Stats` navigation destinations and the never-read `Reader.autoPlay` field from `Destinations.kt` (the Quick Play path now navigates via `SurahPlayer`).
- **StatsScreen:** fixed deprecated icons (`MenuBook`/`DirectionsRun` → `Icons.AutoMirrored.Filled.*`).
- Build verified: `:composeApp:compileDebugKotlinAndroid` and `:composeApp:compileKotlinDesktop`.

### 2026-10-18 (Google Sign-In & Cloud Sync)
- **Stack:** supabase-kt **3.6.0** (`auth-kt`, `postgrest-kt`, `compose-auth`) + Ktor **3.4.3** engines — the newest release built with Kotlin 2.3.21; 3.7+ ship Kotlin 2.4.0 klibs, which this project's compiler cannot consume on iOS. Core-library desugaring keeps `minSdk 24`.
- **Auth:** Google sign-in through Supabase (PKCE). Android uses the native Credential Manager picker (`compose-auth`); Desktop uses the SDK's localhost callback server (default port 48210); iOS falls back to browser OAuth via the `quranlearner://login-callback` deeplink (`iOSApp.swift` + `Info.plist`).
- **Config injection:** `supabase.url` / `supabase.anonKey` / `supabase.googleWebClientId` (+ optional `supabase.desktopRedirectPort`) from `local.properties` or `SUPABASE_*` env vars → generated `SupabaseConfig.kt` (configuration-cache-safe task; editing the file invalidates the cached configuration). Apps built without keys run local-only and hide the cloud UI.
- **Local DB:** all six mutable tables gained `updatedAt` (epoch ms) with an idempotent `ALTER TABLE` + backfill migration — pre-sync data would otherwise never upload. New `sync_tombstones` table records favorite deletions, and re-adding cancels the pending tombstone.
- **Sync engine (`cloud/SyncManager.kt`):** pull-then-push, last-write-wins per row on client `updatedAt`. The pull cursor uses the Postgres-triggered `server_updated_at` (server clock) so device clock skew cannot hide changes; the push watermark is the local sync-start time minus 1 ms (same-millisecond writes are not lost). Rows per table: `user_settings`, `daily_progress`, `ayah_progress`, `achievements`, `ayah_reviews`, `favorites`. Device-only settings (downloaded surahs, export path, sync bookkeeping) never leave the device.
- **UI:** the Settings account card is real — unconfigured hint / "Sign in with Google" / signed-in state (email, "Last synced …", Sync now, Sign out). The app syncs once per account per app run on top of manual sync.
- **Setup artifacts:** `supabase/schema.sql` (tables, `touch_server_updated_at` trigger, pull indexes, RLS `auth.uid() = user_id`, grants) and `supabase/README.md` (Google Cloud OAuth clients incl. Android SHA-1 via `gradlew signingReport`, provider config + authorized client IDs, redirect URLs, `local.properties` keys, troubleshooting).
- **Tests:** `SyncLayerTest` — 9 cases covering timestamping, watermarks, device-only exclusion, legacy-DB backfill, tombstone lifecycle, LWW apply and sync state. 22/22 desktop tests pass.
- Build verified: `:composeApp:assembleDebug`, `:composeApp:compileKotlinDesktop`, `:composeApp:desktopTest`, `:composeApp:compileKotlinIosArm64`, `:composeApp:compileKotlinIosSimulatorArm64`.
- **Pending:** end-to-end verification against a live Supabase project (no credentials available here). The iOS native Google sheet is intentionally not wired (browser fallback instead), and linking the iOS app additionally needs the plugins' `exportedNativeBridge` Swift package — documented in `supabase/README.md`.
