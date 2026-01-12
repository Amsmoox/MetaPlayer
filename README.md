# MetaPlayer - IPTV Player

A professional IPTV player application for Android and Android TV, built with Kotlin and Jetpack Compose.

## Features

- ✅ M3U playlist parsing and loading
- ✅ Live channel streaming with ExoPlayer
- ✅ Modern Material Design 3 UI
- ✅ Android TV support
- ✅ Clean MVVM architecture
- ✅ Coroutines for async operations

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM
- **Video Player**: ExoPlayer (Media3)
- **Networking**: OkHttp, Retrofit
- **Async**: Coroutines + Flow

## Project Structure

```
app/
├── src/main/java/com/metaplayer/iptv/
│   ├── data/
│   │   ├── model/          # Data models (Channel)
│   │   ├── parser/          # M3U parser
│   │   └── repository/      # Data repository
│   ├── presentation/
│   │   ├── ui/
│   │   │   ├── screens/     # Compose screens
│   │   │   └── theme/       # App theme
│   │   └── viewmodel/       # ViewModels
│   └── MainActivity.kt
```

## Getting Started

1. Open the project in Android Studio
2. Sync Gradle files
3. Run on Android device or emulator (API 24+)

## Usage

1. Enter an M3U playlist URL
2. Click "Load Playlist"
3. Select a channel to play
4. Enjoy streaming!

## Requirements

- Android Studio Hedgehog or later
- JDK 17
- Android SDK 24+ (Android 7.0+)
- Target SDK 34

## License

Copyright © 2024 MetaPlayer
