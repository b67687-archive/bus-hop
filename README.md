# BusHop

Lightweight Singapore bus timing app - no geolocation, no clutter.

## Features

- Track bus arrival times for your saved bus stops
- No geolocation required - add stops manually by code
- Shows operator (SBS/SMRT/TTS/GAS) and bus type (DD/SD/BD)
- Light and fast - runs entirely offline with local storage

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- JDK 17
- Android SDK 34

### Build

1. Clone the repository
2. Open in Android Studio
3. Build and run

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Retrofit + OkHttp
- Kotlin Coroutines
- DataStore Preferences

## API

Uses [Arrivelah](https://github.com/cheeaun/arrivelah) API for bus timing data (no API key required).

## License

MIT