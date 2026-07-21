# 📖 Noor — Quran Learning App
## Comprehensive Development Plan (PLAN.md)
**Platform:** Android & iOS (Kotlin Multiplatform + Compose Multiplatform)  
**Date:** May 2026  
**Version:** 1.1.0 — Improved MVP Plan

**Note:** This file is updated when features land or structure changes; ask for a refresh after completing a milestone if you want the checklist and layout kept in sync.

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
| **Networking** | Ktor Client (version catalog) | 3.4.3 | Declared in `libs.versions.toml`; not yet a `composeApp` dependency |
| **Serialization** | Kotlinx Serialization | 1.11.0 | JSON without reflection |
| **Images / assets** | Compose `composeResources` | — | Fonts & drawables bundled with CMP |
| **Navigation** | Navigation Compose KMP | 2.9.2 | Type-safe, KMP-native navigation |
| **Preferences** | SQLite `settings` table (`QuranDb`) | — | Key/value flags (e.g. onboarding); DataStore optional later |
| **Audio (Android)** | ExoPlayer (Media3) | 1.5+ | Robust ayah-by-ayah playback |
| **Audio (iOS)** | AVPlayer | — | Native iOS audio (expect/actual wrapper) |
| **Build** | Gradle | 8.13.2 | Current project version |

---

## 3. Architecture & Directory Structure

Single Gradle module **`composeApp`** (see `settings.gradle.kts`). Shared code lives under **`commonMain`**; platform code under **`androidMain`**, **`iosMain`**, **`desktopMain`**, and **`wasmJsMain`**.

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
│       ├── androidMain/                 # MainActivity, manifest, res, ExoPlayer actual
│       ├── iosMain/                     # MainViewController, AVPlayer actual
│       ├── desktopMain/                 # JVM entry + platform AudioPlayer stub
│       └── wasmJsMain/                  # Wasm AudioPlayer stub
```

**Target layout (not present yet):** A larger refactor might introduce `di/`, `data/` (local + remote), and `domain/` as in earlier sketches; today persistence and queries are concentrated under **`db/`**, and feature logic largely lives in **`ui/screens/*ViewModel`**.

### 3.1 Data Flow & Offline Strategy
- **Pre-population:** On first launch, platforms copy or open a bundled **`pre_populated_quran.db`** (e.g. Android copies from **`assets/`** into app files, then opens with the bundled SQLite driver). iOS/desktop use their own bundle/classpath paths as wired in platform entry code.
- **Offline-First:** Quran text is read from the local database after that first setup.
- **Audio:** Streamed by URL from the reader; optional per-juz download remains a future Download Manager feature.

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
- [x] Achievement system — 15 achievements with full trigger logic in `ReaderViewModel` (milestones, streaks, time/date, surah completion, Juz Amma, marathon, 3 Quls).
- [x] XP & leveling system with daily progress tracking.
- [x] Stats/Profile screen with activity heatmap, achievement grid, privacy dialog, daily goal editor.
- [x] Local push notifications for daily reminders — expect/actual `NotificationScheduler` with Android AlarmManager, iOS UNUserNotificationCenter, desktop/wasm no-op stubs. Reminder toggle + time picker UI in Profile screen.

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
- [x] **Local Push Notifications:** `expect/actual` `NotificationScheduler` — Android (AlarmManager + BroadcastReceiver + NotificationCompat), iOS (UNUserNotificationCenter), Desktop/Wasm (no-op stubs). Reminder toggle + quick-select time picker in Profile Settings. Settings persisted in DB.
- [x] **Search:** Search bar in `SurahListScreen` with `filteredSurahs` Flow in ViewModel. Filters by English name, Arabic name, translation meaning, or surah number. Animated clear button, empty-state messaging. Also migrated entire SurahListScreen to NoorColors.

### Medium Priority
- [x] **Favorites / Bookmarks:** Heart toggle on `AyahCard` in the Reader. Separate `FavoritesScreen` accessible from Profile hub. DB `favorites` table with toggle/query/count methods.
- [x] **Reciter Selection:** `SettingsScreen` with 8 reciters (Alafasy, Husary, Minshawi, Abdulbasit, Sudais, Shuraim, Ajamy, Maher). Persisted `reciter_id` in DB. Dynamic `playAyahAudio()` using reciter folder. Dock subtitle shows selected reciter name.
- [x] **Font Size Sliders:** Arabic (24–56sp) and translation (12–28sp) sliders in `SettingsScreen`. Values persisted via `SettingsViewModel`.
- [ ] **Download Manager:** Per-surah or per-juz offline audio download. Store in app-local cache. (Placeholder UI added in Settings.)

### Low Priority / Future
- [ ] **Supabase Auth & Cloud Sync:** Wire up the existing Sign In placeholder. Sync learned ayahs, achievements, and XP to Supabase.
- [ ] **Tajweed Highlights:** Color-coded text for Uthmani script (requires annotated text data or Tajweed API).
- [ ] **Focus Mode Refinement:** Gesture-based controls, swipe to advance ayahs.
- [ ] **iOS-specific polish:** AVPlayer audio wrapper testing, platform-specific notification permissions.

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
