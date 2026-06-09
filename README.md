<div align="center">
  <h1>🚌 BusHop-SG</h1>
  <p><strong>Lightweight Singapore bus timing app</strong></p>
  <p>Material 3 Compose UI with real-time arrivals, drag-to-reorder, pinning, and smart search.</p>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Compose-BOM%202024-4285F4?logo=jetpackcompose&logoColor=white">
    <img src="https://img.shields.io/badge/minSdk-24-34A853">
    <img src="https://img.shields.io/badge/targetSdk-34-34A853">
    <img src="https://img.shields.io/badge/license-MIT-yellow">
    <img src="https://img.shields.io/badge/tests-146%20passing-34A853">
  </p>
</div>

---

## Screenshots

| Main Screen                                        | Drag to reorder                                            | Delete zone                                      | Search & Add                                            |
| -------------------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------- |
| _Bus stop list with arrivals, pinned stops at top_ | _Long-press a stop, drag to reposition, release to commit_ | _Drag into the red zone at the bottom to delete_ | _Type-stop search with fuzzy matching and nearby stops_ |
| _(screenshot needed)_                              | _(screenshot needed)_                                      | _(screenshot needed)_                            | _(screenshot needed)_                                   |

## Features

|     | Feature                  | Detail                                                                                    |
| --- | ------------------------ | ----------------------------------------------------------------------------------------- |
| 🚌  | **Real-time arrivals**   | Shows next 3 buses per service with minutes-to-arrival                                    |
| 🏷️  | **Operator badges**      | SBS, SMRT, TTS, Go-Ahead colour-coded                                                     |
| 🚍  | **Bus type icons**       | Single Decker, Double Decker, Bendy                                                       |
| 💺  | **Load indicator**       | Seats Available / Standing Available / Limited Standing                                   |
| ♿  | **Wheelchair info**      | Wheelchair Accessible Bus (WAB) indicator                                                 |
| 📌  | **Pin stops & services** | Pin stops to the top; pin individual bus services within a stop                           |
| 🔍  | **Smart search**         | Tokenized search with Trie prefix matching + Levenshtein fuzzy matching                   |
| 📍  | **Nearby stops**         | Location-based nearby stop finder (opt-in)                                                |
| 🌙  | **Theme support**        | Light, Dark, System-following, with Blue and Contrast Blue colour schemes — all persisted |
| 🔄  | **Auto-refresh**         | Configurable interval (30s / 1m / 2m / 5m / Off) — pauses in background                   |
| ↘️  | **Pull to refresh**      | Swipe down to refresh all stops                                                           |
| 🖱️  | **Drag to reorder**      | Long-press and drag bus stops to reorder — items shift dynamically during drag            |
| 🗑️  | **Drag to delete**       | Drag a stop into the bottom delete zone — card-center-in-zone threshold                   |
| 🔒  | **Privacy first**        | Location is opt-in only. No accounts, no analytics, no telemetry                          |
| 📱  | **Material 3**           | Modern Compose UI with animations, pull-to-refresh, edge-to-edge                          |

## Download

> **Latest release:** [v1.0.0](https://github.com/B67687/BusHop-SG/releases/latest) — `bus-hop.apk` (**1.9 MB**, R8-minified release build)

Or [build from source](#build-from-source).

## Architecture

```
┌──────────────────────────────────────────────┐
│              App Module (app/)                │
│  ┌─────────┐  ┌──────────┐ ┌──────────────┐  │
│  │  UI     │  │ ViewModel│ │  Components   │  │
│  │(Compose)│◄─┤ (State)  │◄─┤ (Theme/Dialogs)│ │
│  └─────────┘  └──────────┘ └──────────────┘  │
├──────────────────────────────────────────────┤
│             Data Module (data/)                │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐  │
│  │  API     │ │  Local   │ │ BusRepository │  │
│  │(Retrofit)│ │(DataStore)│ │   Impl       │  │
│  │ + Trie   │ │+ Index   │ │              │  │
│  └──────────┘ └──────────┘ └──────────────┘  │
├──────────────────────────────────────────────┤
│            Domain Module (domain/)             │
│  ┌───────────┐ ┌────────────┐ ┌───────────┐  │
│  │  Models   │ │  UseCases  │ │ Repository│  │
│  │ (no deps) │ │            │ │ Interface │  │
│  └───────────┘ └────────────┘ └───────────┘  │
└──────────────────────────────────────────────┘
```

- **domain/** — Pure Kotlin (zero framework deps). Models, use cases, repository interface.
- **data/** — Android library. Retrofit API calls, DataStore persistence, BusStopIndex with TokenTrie for search.
- **app/** — Android app. Jetpack Compose UI, ViewModels, theme, components.

## Tech Stack

| Layer         | Technology                                                |
| ------------- | --------------------------------------------------------- |
| Language      | Kotlin 2.0                                                |
| UI            | Jetpack Compose (BOM 2024.10) + Material 3                |
| Architecture  | MVVM + Clean Architecture (3 modules)                     |
| Networking    | Retrofit 2 + OkHttp 4                                     |
| Serialization | Gson (data layer only)                                    |
| Persistence   | DataStore Preferences                                     |
| Async         | Kotlin Coroutines 1.8 + Flow                              |
| DI            | Manual constructor injection                              |
| Search        | Inverted index + TokenTrie (prefix) + Levenshtein (fuzzy) |
| Testing       | JUnit 4, MockK, Coroutines Test                           |
| Minification  | R8 + ProGuard (release builds)                            |
| Target        | Android 14 (SDK 34), min SDK 24                           |

## Build from Source

### Prerequisites

- **JDK 17** (OpenJDK)
- **Android SDK 34** with build tools
- Set `ANDROID_HOME` to your SDK path

### Commands

```bash
# Debug build + tests + APK verification
./gradlew clean test checkAndRenameDebugApk

# Release build
./gradlew assembleRelease

# APK output at:
# app/build/outputs/apk/debug/bus-hop.apk
```

## Automated Checks

| Check              | When                            | Where                                                                |
| ------------------ | ------------------------------- | -------------------------------------------------------------------- |
| APK integrity      | Every `./gradlew assembleDebug` | `app/build.gradle.kts` — `checkAndRenameDebugApk`                    |
| Lint + Tests + APK | Every `git push`                | `.github/workflows/ci.yml`                                           |
| Architecture tests | Every `./gradlew test`          | `ArchitectureTest.kt` — layer separation, ProGuard, dependency rules |

## Testing

**146 tests** across 7 test files:

| Module                        | Tests | What's covered                                                    |
| ----------------------------- | ----- | ----------------------------------------------------------------- |
| Domain: UseCase               | 22    | sortServices, sortServicesWithPins, applyPinning, toggleCollapsed |
| Domain: Model                 | 10    | toDisplayArrival eta/load/busType mapping                         |
| Domain: RefreshCoordinator    | 6     | Cooldown, independent cooldowns, concurrent batching              |
| Domain: AutoRefreshController | 7     | Start/stop/restart/onCleared lifecycle                            |
| Data: BusStopIndex            | 45    | Search (exact, prefix, fuzzy, abbreviations, sorting, findNearby) |
| Data: RetryUtil               | 6     | Retry with backoff, CancellationException propagation             |
| App: MainViewModel            | 50+   | add/remove/move/pin/collapse/refresh/sort/errors                  |
| App: Architecture             | 4     | Layer separation, minification, dependency rules, ProGuard        |

## API

BusHop uses the [Arrivelah](https://github.com/cheeaun/arrivelah) API (`arrivelah2.busrouter.sg`), which proxies LTA DataMall's BusArrivalv2 endpoint. No API key required.

## Privacy

| Data          | Collected?                                |
| ------------- | ----------------------------------------- |
| Location      | 🔘 — opt-in, never sent off-device        |
| Personal info | ❌ — no accounts, no sign-in              |
| Analytics     | ❌ — no tracking SDKs                     |
| Crash reports | ❌ — not integrated                       |
| Saved stops   | 🔒 — stored locally in DataStore          |
| API calls     | 🔒 — direct to BusRouter, no intermediary |

## License

MIT License — see [LICENSE](LICENSE).
