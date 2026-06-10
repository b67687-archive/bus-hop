<div align="center">
  <img src="icon.svg" alt="BusHop" width="96" height="96">
  <h1>BusHop</h1>
  <p><strong>Lightweight Singapore bus timing app</strong></p>
  <p>Material 3 Compose UI with real-time arrivals, drag-to-reorder, pinning, and smart search. No ads, no accounts, no tracking.</p>
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Compose-BOM%202024-4285F4?logo=jetpackcompose&logoColor=white">
    <img src="https://img.shields.io/badge/minSdk-24-34A853">
    <img src="https://img.shields.io/badge/targetSdk-34-34A853">
    <img src="https://img.shields.io/badge/license-MIT-yellow">
    <img src="https://img.shields.io/badge/tests-154%20passing-34A853">
  </p>
</div>

---

## Screenshots

| Main Screen                                        | Drag to reorder                                            | Delete zone                                      | Search & Add                                            |
| -------------------------------------------------- | ---------------------------------------------------------- | ------------------------------------------------ | ------------------------------------------------------- |
| _Bus stop list with arrivals, pinned stops at top_ | _Long-press a stop, drag to reposition, release to commit_ | _Drag into the red zone at the bottom to delete_ | _Type-stop search with fuzzy matching and nearby stops_ |
| ![Main screen](screenshot_stops.png)               | _(screenshot needed)_                                      | _(screenshot needed)_                            | ![Search dialog](screenshot_search.png)                 |

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
| 🖱️  | **Drag to reorder**      | Long-press and drag bus stops to reorder — commit on drag end                             |
| 🗑️  | **Drag to delete**       | Drag a stop into the bottom delete zone — card-center-in-zone threshold                   |
| 🔒  | **Privacy first**        | Location is opt-in only. No accounts, no analytics, no telemetry                          |
| 📱  | **Material 3**           | Modern Compose UI with animations, pull-to-refresh, edge-to-edge                          |

## Download

> **Latest release:** [v1.0.0](https://github.com/B67687/BusHop/releases/latest) — `bus-hop.apk` (**2 MB**, R8-minified, signed)

Or [build from source](#build-from-source) for a debug APK.

## Architecture

```mermaid
graph TD
    subgraph app["app/ — Android App"]
        UI["UI (Compose)"] --> VM["ViewModel (State)"]
        VM --> C["Components<br/>(Theme, Dialogs)"]
    end

    subgraph data["data/ — Data Layer"]
        API["API<br/>(Retrofit + Arrivelah)"] --> REPO["BusRepositoryImpl"]
        LOCAL["Local<br/>(DataStore + Index)"] --> REPO
        TRIE["TokenTrie<br/>(Search Index)"] --> LOCAL
    end

    subgraph domain["domain/ — Pure Kotlin"]
        MODELS["Models<br/>(no deps)"]
        UC["UseCases"]
        IFACE["Repository Interface"]
    end

    VM --> UC
    UC --> IFACE
    REPO --> IFACE
    IFACE --- MODELS
```

- **domain/** — Pure Kotlin (zero framework deps). Models, use cases, repository interfaces.
- **data/** — Android library. Retrofit API calls, DataStore persistence, BusStopIndex with TokenTrie for search.
- **app/** — Android app. Jetpack Compose UI, ViewModels, theme, components.

## Pipeline

<img src="pipeline.svg" alt="Development pipeline" width="800">

1. **Development** — Code written iteratively by AI agent + human review. Source, tests, and config live in `main`.
2. **CI** — Every push triggers linting, 154 unit tests, and architecture boundary checks via GitHub Actions.
3. **Build** — Gradle compiles Kotlin, R8 minifies + optimizes the release APK down to ~2 MB (vs 18 MB debug).
4. **Release** — APK is SSH-signed, published as a GitHub Release, and distributed via Obtainium for automatic updates.
5. **History** — `git filter-repo` periodically cleans agent tooling artifacts (workflows, session state) from git history, keeping only app-relevant commits.

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

**154 tests** across 8 test files:

| Module                        | Tests | What's covered                                                    |
| ----------------------------- | ----- | ----------------------------------------------------------------- |
| Domain: UseCase               | 22    | sortServices, sortServicesWithPins, applyPinning, toggleCollapsed |
| Domain: Model                 | 10    | toDisplayArrival eta/load/busType mapping                         |
| Domain: RefreshCoordinator    | 6     | Cooldown, independent cooldowns, concurrent batching              |
| Domain: AutoRefreshController | 7     | Start/stop/restart/onCleared lifecycle                            |
| Domain: BusStopUseCase        | 4     | addFavoriteStop, removeFavoriteStop, getSavedStops, refresh       |
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
