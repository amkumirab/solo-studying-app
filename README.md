# Solo Studying

Solo Studying is an offline Android study timer that turns learning goals into
RPG-style campaigns. A study session damages a selected boss, earns experience
and gold, and records progress against the skill being trained.

The project is a work in progress. Its current focus is a reliable local-first
gameplay loop rather than accounts, cloud sync, or online services.

## What works

- First-run setup for goals, study days, and daily targets
- Dungeons for grouping related goals
- Boss battles and free-study sessions
- Pause, resume, abandon, and completion flows
- Independent skill progression based on focused time
- Custom real-life rewards purchased with earned gold
- Streaks, level progression, and recovery challenges
- Local reminders based on schedule and current progress
- Low-latency sci-fi interface sounds for study and progression feedback
- Persistent sound enable and volume controls
- Local persistence with Room

## Tech stack

- Kotlin
- Jetpack Compose and Material 3
- Room with Kotlin Symbol Processing
- Coroutines, Flow, and StateFlow
- MVVM with feature-specific view models
- AlarmManager and BroadcastReceiver for reminders
- Robolectric and JUnit for local tests

The bundled interface sounds come from Kenney's CC0 Interface Sounds pack.
See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md) for the source, license,
and file mapping.

## Architecture

```text
Compose screens
      |
SoloStudyingViewModel
      |
Feature view models (battle, status, dungeon, skill, shop)
      |
SoloStudyingRepository
      |
Room DAO and SQLite database
```

`SoloStudyingViewModel` is the interface used by the Compose layer. It delegates
feature behavior to smaller view models while the repository keeps database
access in one place.

## Run locally

### Requirements

- Android Studio with support for Android Gradle Plugin 9.1
- Android SDK 36.1
- JDK 21 (the JDK bundled with current Android Studio works)
- An emulator or Android device running API 24 or newer

### Setup

1. Clone the repository:

   ```bash
   git clone https://github.com/amkumirab/solo-studying-app.git
   cd solo-studying-app
   ```

2. Open the folder in Android Studio.
3. Allow Gradle to sync and download the declared dependencies.
4. Select the `app` configuration and run it on an emulator or device.

No API key or online account is required. Study data stays in the app's local
Room database.

## Tests

Run the local JVM test suite from Android Studio or the project root.

macOS/Linux:

```bash
bash ./gradlew testDebugUnitTest
```

Windows:

```powershell
.\gradlew.bat testDebugUnitTest
```

The core test suite covers onboarding, persistence, boss progress, rewards,
streak behavior, and tutorial completion.

## Build and install a debug APK

On Windows, build the app from the repository root:

```powershell
.\gradlew.bat lintDebug assembleDebug
```

The APK is created at `app/build/outputs/apk/debug/app-debug.apk`. With USB
debugging enabled and the device authorized, install or update it with:

```powershell
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Repository layout

```text
app/src/main/java/com/amkumirab/solostudying/
├── data/
│   ├── dao/
│   ├── database/
│   ├── entity/
│   └── repository/
├── notification/
├── sound/
└── ui/
    ├── screens/
    ├── theme/
    └── viewmodel/
```

Product rules and implementation boundaries are documented in
[`docs/PRODUCT_OVERVIEW.md`](docs/PRODUCT_OVERVIEW.md).

## Current limitations

- Data is stored on one device; export and sync are not implemented.
- Reminder times are currently defined by the app rather than edited in a
  dedicated settings screen.
- Some Compose screens are still large and will be split into feature files as
  the UI evolves.
- Release signing is intentionally left to the developer's local configuration.

## Next steps

- Split the main Compose screen by feature
- Add editable reminder times
- Add data export and restore
- Improve accessibility labels and UI tests
- Add screenshots and a short demo recording

## License

Copyright (c) 2026 Amir Ali Mirab Zadeh Ardekani. All rights reserved.

The source code may be downloaded, compiled, installed, and run only for
personal, non-commercial evaluation and testing. Modification, redistribution,
publication, commercial use, and derivative works are not permitted without
prior written permission. See the [Evaluation-Only License](LICENSE) for the
complete terms. This is not an open-source license.
