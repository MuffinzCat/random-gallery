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
| `versionCode` | 12    |
| `versionName` | 1.2.0 |

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

## Project structure

- Root Gradle project (`settings.gradle.kts`, version catalog `gradle/libs.versions.toml`), single `app/` module
- Compose UI in `app/src/main/java/com/lvlh2/randomgallery/`
- Repos: google() + mavenCentral() (Aliyun mirror fallback if blocked)

## Toolchain

| Component              | Version  |
| ---------------------- | -------- |
| Gradle                 | 9.3.1 (wrapper only — no system Gradle) |
| AGP                    | 8.12.0   |
| Kotlin                 | 2.1.20   |
| JDK                    | 17       |
| compileSdk / targetSdk | 36       |
| minSdk                 | 24       |

Feature roadmap (from README): random shuffle grid, SAF folder import, full-screen viewer with pinch-zoom. See README.md.
