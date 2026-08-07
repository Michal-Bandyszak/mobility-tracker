# Mobility Tracker

Offline-first Android app for following a structured mobility challenge and logging a two-day strength programme.

The project was built as a practical example of a complete mobile product flow: local persistence, domain rules, workout scheduling, progress analytics, notifications, and a Compose UI that remains useful without an account or backend.

## What it does

- Guides a **12-week mobility challenge** with 84 sessions and a repeating 17-video sequence.
- Keeps the next unfinished session active, so missing a day does not silently skip training.
- Plays locally stored mobility videos resolved through Android `MediaStore`.
- Provides a configurable **two-day strength plan** with exercise alternatives, warm-up sets, working-set logging, and technique links.
- Lets the user assign strength days to weekdays and view the plan on the Today screen.
- Shows challenge progress, streaks, weekly goals, cycle history, strength sessions, and exercise trends.
- Sends daily reminders and supports marking a session complete directly from the notification.
- Preserves strength history when the mobility challenge is reset.
- Works fully offline with no account, backend, or analytics SDK.

## Portfolio highlights

This repository demonstrates:

- Jetpack Compose UI with Material 3 and system-aware dark mode.
- MVVM-style presentation with a separate domain layer and focused use cases.
- Room persistence with explicit entities, DAOs, repositories, and migrations.
- Hilt dependency injection and type-safe Compose Navigation.
- Android lifecycle work: reboot-safe reminders, notification actions, runtime permissions, and local media access.
- Business rules covered by unit tests, including missed mobility days, challenge limits, streaks, workout substitutions, and strength-set semantics.

## Architecture

```text
Compose screens
      ↓
ViewModels + UI state
      ↓
Domain use cases
      ↓
Repositories
      ↓
Room / MediaStore / AlarmManager
```

The app keeps workout rules in the domain layer instead of embedding them in composables. This makes the most important behaviour—session progression, completion, reset, and strength statistics—testable without an emulator.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation Compose |
| Architecture | ViewModels, domain use cases, repositories |
| Persistence | Room / SQLite |
| Dependency injection | Hilt |
| Notifications | AlarmManager, BroadcastReceiver, NotificationCompat |
| Local media | Android MediaStore |
| Minimum Android version | API 26 (Android 8.0) |
| Compile / target SDK | 35 |

## Run locally

### Requirements

- Android Studio with an Android SDK for API 35.
- JDK 11 (Android Studio's bundled JBR works).
- Android device or emulator running API 26+.

Clone and open the repository in Android Studio:

```bash
git clone https://github.com/Michal-Bandyszak/mobility-tracker.git
cd mobility-tracker
```

Build the debug APK from the terminal:

```bash
./gradlew assembleDebug
```

Run the unit tests:

```bash
./gradlew test
```

Install the debug build on an authorized USB device:

```bash
./gradlew installDebug
```

## Local video setup

The training videos are intentionally not committed to the repository. Add the 17 files to the device's video storage, for example `Movies/` or `Download/`. The app finds them by filename through `MediaStore`, using case-insensitive matching and Polish-character normalization.

The expected names are defined in [`WorkoutPlan.kt`](app/src/main/java/com/mobilitytracker/data/model/WorkoutPlan.kt). The app can still show the training plan when a video is missing; only playback is unavailable for that session.

## Product flow

1. Choose the programme start date on first launch.
2. Open **Today** to see the active mobility session and any scheduled strength session.
3. Complete the mobility video once per calendar day.
4. Log working sets during strength training; optional warm-up logs are excluded from progress totals.
5. Use **Calendar** and **Stats** to review consistency and progression.
6. Configure weekdays and reminder time in **Settings**.

The mobility sequence repeats every 17 sessions, while the challenge milestone is reached after 84 completed sessions. The active session advances only when the current session is completed, which makes the flow resilient to missed days.

## Permissions

| Permission | Purpose |
| --- | --- |
| `READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE` | Read locally stored training videos |
| `POST_NOTIFICATIONS` | Show training reminders on Android 13+ |
| `SCHEDULE_EXACT_ALARM` | Deliver reminders at the configured time |
| `RECEIVE_BOOT_COMPLETED` | Restore reminders after reboot or app update |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Improve reminder reliability on some devices |

## Scope and limitations

- This is a local Android app; there is no sync between devices.
- Video files are user-provided and are not distributed with the project.
- Reminder delivery can still be affected by aggressive manufacturer battery policies.
- There is no release distribution or hosted demo in this repository; the intended portfolio artifact is the source code and a locally installable debug build.

## License

MIT
