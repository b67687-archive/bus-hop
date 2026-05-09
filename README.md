<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png" alt="BusHop-SG" width="96" height="96">
  <h1>BusHop-SG</h1>
  <p><strong>Lightweight Singapore bus timing app</strong></p>
  <p>No geolocation. No clutter. Just bus arrival times.</p>
  <!-- Badges -->
  <p>
    <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?logo=kotlin&logoColor=white">
    <img src="https://img.shields.io/badge/Compose-BOM%202024-4285F4?logo=jetpackcompose&logoColor=white">
    <img src="https://img.shields.io/badge/minSdk-24-34A853">
    <img src="https://img.shields.io/badge/targetSdk-34-34A853">
    <img src="https://img.shields.io/badge/license-MIT-yellow">
  </p>
</div>

---

## Features

| | Feature | Detail |
|---|---------|--------|
| 🚌 | **Real-time arrivals** | Shows next 3 buses per service with minutes-to-arrival |
| 🏷️ | **Operator badges** | SBS, SMRT, TTS, Go-Ahead colour-coded |
| 🚍 | **Bus type icons** | Single Decker, Double Decker, Bendy |
| 💺 | **Load indicator** | Seats Available / Standing Available / Limited Standing |
| ♿ | **Wheelchair info** | Wheelchair Accessible Bus (WAB) indicator |
| 📌 | **Pin stops & services** | Pin favourite stops to the top, pin specific services within a stop |
| 🔍 | **Smart search** | Find bus stops by code or name — scored relevance, digit fast-path |
| 🌙 | **Theme support** | Light, Dark, and System-following — persisted across restarts |
| 🔄 | **Auto-refresh** | Configurable interval (30s / 1m / 2m / 5m / Off) — pauses in background |
| ↘️ | **Pull to refresh** | Swipe down to refresh all stops |
| 🔒 | **Privacy first** | No geolocation, no account, no analytics — all data stored locally |
| 📱 | **Material 3** | Modern Compose UI with animations, pull-to-refresh, edge-to-edge |

## Screenshots

<!-- Add screenshots here once available -->

| Light | Dark | Search | Settings |
|-------|------|--------|----------|
| *TODO* | *TODO* | *TODO* | *TODO* |

## Download

> **Latest release:** [v0.6.7](https://github.com/B67687/BusHop-SG/releases/latest) — `app-debug-bus-hop.apk` (17 MB)

Or build from source (see below).

## Architecture

```
┌─────────────────────────────────────────────┐
│                  App Module                  │
│  ┌─────────┐ ┌──────────┐ ┌──────────────┐  │
│  │  UI     │ │ ViewModel│ │  Components   │  │
│  │(Compose)│◄┤ (State)  │◄┤ (Theme/Dialogs)│  │
│  └─────────┘ └──────────┘ └──────────────┘  │
├─────────────────────────────────────────────┤
│               Data Module                    │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐  │
│  │  API     │ │  Local   │ │ Repository   │  │
│  │(Retrofit)│ │(DataStore)│ │  Impl       │  │
│  └──────────┘ └──────────┘ └─────────────┘  │
├─────────────────────────────────────────────┤
│              Domain Module                   │
│  ┌──────────┐ ┌──────────┐ ┌─────────────┐  │
│  │  Models  │ │ UseCases │ │ Repository   │  │
│  │          │ │          │ │  Interface   │  │
│  └──────────┘ └──────────┘ └─────────────┘  │
└─────────────────────────────────────────────┘
```

- **domain/** — Pure Kotlin module. No Android dependencies. Contains models, use cases, repository interface.
- **data/** — Android module. API calls (Retrofit), local persistence (DataStore), repository implementation.
- **app/** — Android module. Jetpack Compose UI, ViewModels, theme, dialogs and components.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 1.9 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture (3 modules) |
| Networking | Retrofit 2 + OkHttp |
| Serialization | Gson |
| Persistence | DataStore Preferences |
| Async | Kotlin Coroutines + Flow |
| DI | Manual constructor injection (no DI framework) |
| Testing | JUnit 4, MockK, Coroutines Test |
| Minification | R8 + ProGuard (release builds) |
| Target | Android 14 (SDK 34), min SDK 24 |

## Build from Source

### Prerequisites

- **JDK 17** (OpenJDK)
- **Android SDK 34** with build tools 34.0.0
- Set `ANDROID_HOME` to your SDK path

### Commands

```bash
# Clone
git clone https://github.com/B67687/BusHop-SG.git
cd BusHop-SG/bus-hop-content

# Debug build + tests
./gradlew assembleDebug testDebugUnitTest

# Release build
./gradlew assembleRelease

# APK output at:
# app/build/outputs/apk/debug/app-debug-bus-hop.apk
```

### Environment

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=$HOME/Android/Sdk
```

## API

BusHop uses the [Arrivelah](https://github.com/cheeaun/arrivelah) API (`arrivelah2.busrouter.sg`), which proxies LTA DataMall's BusArrivalv2 endpoint. No API key required.

The app also includes a data source for the official LTA DataMall API (API key required).

## Privacy

BusHop does not collect any data:

| Data | Collected? |
|------|-----------|
| Location | ❌ — never requested |
| Personal info | ❌ — no accounts, no sign-in |
| Analytics | ❌ — no tracking SDKs |
| Crash reports | ❌ — not integrated |
| Saved stops | 🔒 — stored locally in DataStore |
| API calls | 🔒 — direct to BusRouter / LTA, no intermediary |

## License

MIT License

Copyright (c) 2024 B67687

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

...

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Before submitting

```bash
./gradlew assembleDebug testDebugUnitTest
```

Ensure all 36+ tests pass and the build is green.

---

<div align="center">
  <sub>Built with ❤️ for Singapore commuters</sub>
</div>
