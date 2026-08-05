# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Identity

Native Android app written in **Kotlin + Jetpack Compose** — a full rewrite of the previous Expo/React Native app. All Expo code (JS `src/`, 116 JS tests, configs) was deleted; **recoverable via git history** (before the restructure commit). Do not reintroduce Node tooling.

## Build & Run

```bash
./gradlew assembleDebug     # debug APK
./gradlew assembleRelease   # release APK
```

APK output: `app/build/outputs/apk/{debug,release}/random-gallery.apk` (all variants renamed to the same filename). Use `gradlew.bat` in cmd/PowerShell; `./gradlew` works in Git Bash. Android Studio recommended as IDE.

## Identity invariants — never break

- `applicationId`/`namespace` = `com.lvlh2.randomgallery`
- `app/debug.keystore` signs **both** debug and release (the old release was signed with this debug key — this is why install-over works). Never replace or lose it; never introduce a different signing key.
- `versionCode` must strictly increase vs what's installed on the user's phone.

## Version bump

Edit **only** `versionCode` and `versionName` in `app/build.gradle.kts` (single source of truth, tracked in git), then `./gradlew assembleRelease`.

**Current values — UPDATE THESE when bumping:**

| Field         | Value |
| ------------- | ----- |
| `versionCode` | 20    |
| `versionName` | 2.0.0 |

(The old app required manually syncing THREE values across app.json / android build.gradle, and `expo prebuild` silently reset versionCode to 1 — that failure mode no longer exists.)

## Lint & Format

```bash
./gradlew ktlintCheck      # Kotlin style check (required before commit)
./gradlew ktlintFormat     # auto-fix
./gradlew lint             # Android Lint
```

## Tests

```bash
./gradlew test             # JUnit unit tests in app/src/test/
```

Write tests when adding feature logic.

## Architecture

Single `app/` module, packages under `app/src/main/java/com/lvlh2/randomgallery/`:

- **`ui/`** — `GalleryViewModel` (one activity-scoped `AndroidViewModel`, all state in `StateFlow<GalleryUiState>`: folders / images / loading / error / screen). Hand-rolled navigation: `sealed Screen { Tabs, Viewer(index) }` + `BackHandler` — no navigation library. `RandomGalleryApp.kt` is the root composable (tabs scaffold + ON_RESUME re-scan observer).
- **`ui/screens/`** — `RandomScreen` (3-col `LazyVerticalGrid`, keyed by URI so reshuffle reorders in place), `FoldersScreen` (SAF import + checkbox rows), `ViewerScreen`/`ViewerPage` (HorizontalPager + telephoto zoom + swipe gestures).
- **`data/`** — `ImageRepository` (SAF tree enumeration via `DocumentsContract`), `FolderStore` (SharedPreferences + kotlinx-serialization, one JSON string), pure list helpers in `FolderRepository`.
- **`util/`** — pure string/collection logic (shuffle, extension filter, tree-uri parsing, delete-index math) — deliberately Android-free so unit tests run on the JVM.

Key behaviors: images are read in place via SAF, never copied; pure-SAF means **no runtime permissions** (manifest declares none). Shuffle is Fisher-Yates once per scan; ↻ re-shuffles the current list only. Delete = `DocumentsContract.deleteDocument`, permanent, removed from list only on success; deleting the last image scrolls the pager *before* the list shrinks (4d3c1c8 fix), deleting the only image closes the viewer.

**Viewer gestures** (highest-risk area): telephoto's `canPan` only consumes drags when zoomed in, so at scale 1 a `detectVerticalDragGestures` on the page receives the events — swipe up/down past 120dp = delete/exit. Zoomed in, the detector cancels itself via `zoomFraction` check. Verified on emulator.

## Toolchain

| Component              | Version  |
| ---------------------- | -------- |
| Gradle                 | 9.3.1 (wrapper only — no system Gradle) |
| AGP                    | 8.12.0   |
| Kotlin                 | 2.1.20   |
| JDK                    | 17       |
| compileSdk / targetSdk | 36       |
| minSdk                 | 24       |
| Coil (coil-compose)    | **3.2.0** — do NOT bump: 3.3+ needs Kotlin 2.2, 3.5.0 needs 2.4.0 (stdlib metadata breaks the 2.1.20 compiler) |
| telephoto (zoomable-image-coil3) | 0.19.0 — compiled against Coil 3.2.0; API note: `ZoomableAsyncImage` lives in `me.saket.telephoto.zoomable.coil3`, `contentDescription` is required, zoom state is `zoomState.zoomableState.zoomFraction` |
| kotlinx-serialization-json | **1.8.1** — do NOT bump: 1.9+ needs Kotlin 2.2 |
| androidx.lifecycle (runtime-ktx / runtime-compose / viewmodel-compose) | 2.9.0 — keep one release train |

ktlint plugin 13.1.0 enforces ktlint 2.x style (params on own lines, no blank line after `{`, etc.) — run `ktlintFormat` before fighting `ktlintCheck` by hand.

## Emulator testing (adb)

```bash
adb connect 127.0.0.1:5555     # the test emulator (swappable ROM images, API 32)
adb install -r app/build/outputs/apk/debug/random-gallery.apk
adb exec-out uiautomator dump /dev/tty   # drive/verify UI without screenshots
```

Note: `screencap` works but PNGs can't be viewed by Claude here — use uiautomator dump (text + bounds) and PIL pixel analysis instead. Pinch-zoom can be simulated with `sendevent` multi-touch on `/dev/input/event4` (script in git history / write a new one). 2000-image perf corpus lives at `/sdcard/Pictures/perf2000/` on the emulator. Emulator specifics learned in testing: **screen resolution follows the ROM image** (1080x1920 vs 1440x2560) — always derive tap targets from uiautomator bounds, never hardcode coordinates; **`adb push` of a large directory can silently drop files** (verify with `ls | wc -l` and re-push stragglers); switching the ROM image keeps userdata (app + corpus) but drops URI grants (see quirks above).

## Known platform quirks (learned the hard way)

- **`buildDocumentUriUsingTree` is implemented *by the ROM* — its output shape and the provider's parsing are always from the same vendor codebase, so they match on every device** (standard AOSP returns `/document/<docId>`; the vivo ROM tested here returns a tree-shaped `content://.../tree/<treeId>/document/<docId>` and its provider parses that). Always build item URIs through this helper — never hand-construct the AOSP "standard" shape, which loads fine on AOSP but shows a black grid on the vivo ROM. Regression-tested end-to-end (import → grid → delete → restart) on **three emulator ROM families: vivo V2185A, OPPO PJJ110, Samsung SM-S9210** — load/delete/persistence all pass. Known vendor quirks: vivo's provider intermittently fails deletes ("Failed to delete" for files it previously reported deleted — snackbar + keep-in-list path handles it, other ROMs are stable); OPPO's provider serves READS without a URI grant but rejects DELETEs without WRITE.
- **URI grants are per-system-instance, not per-app-data**: switching ROMs (or restoring app data onto a new device) drops them. App data (folder list) survives, deletes then fail silently. `importFolder` therefore re-takes the persistable permission when a uri is already imported ("Folder already imported — access restored") instead of rejecting — never make re-import a no-op.
- `URLDecoder.decode(String, Charset)` is Java 10+ (Android API 33+); `TreeUriUtils` uses the `(String, String)` overload for API 24-32. JVM unit tests won't catch API-level gaps — verify on the emulator.

Feature roadmap (from README) is implemented: random shuffle grid, SAF folder import with checkboxes, full-screen viewer with pinch-zoom / swipe-up delete / swipe-down exit. See README.md.
