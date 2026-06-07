# MobilityTracker

Android app for tracking a scheduled mobility program and a 2-day strength training plan. Built with Jetpack Compose and modern Android architecture.

## Features

- **Scheduled mobility tracking** — configurable weekdays for a repeating 17-video mobility cycle
- **Strength training plan** — 2-day gym plan with exercise prescriptions, technique links, and set logging
- **Set logs** — save weight and completed reps per exercise and set
- **Training schedule** — choose which weekdays are for mobility and which are for strength training
- **Flow reset** — reset program start, mobility completions, and strength logs while keeping preferences
- **Calendar view** — navigate between cycles, see completion history at a glance
- **Statistics** — animated progress ring, streaks, per-cycle breakdown, category distribution
- **Training reminders** — configurable push notifications that survive reboots and app updates
- **Quick action** — mark workout as done directly from the notification, without opening the app
- **Dark mode** — automatic system-based theming with Material 3
- **Fully offline** — no backend, no account required

## Screenshots

> _(add screenshots here)_

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Navigation | Compose Navigation |
| Architecture | MVVM + Clean Architecture |
| Database | Room (SQLite) |
| Dependency Injection | Hilt |
| Notifications | AlarmManager + NotificationCompat |
| Language | Kotlin |
| Min SDK | 26 (Android 8.0) |

## Project Structure

```
app/src/main/java/com/mobilitytracker/
├── data/
│   ├── local/          # Room database, DAOs
│   ├── model/          # Entities, mobility plan, strength plan, schedules
│   ├── repository/     # Data access layer
│   └── video/          # MediaStore video URI resolution
├── domain/
│   ├── model/          # UI/domain models (DayWorkout, CycleStats)
│   └── usecase/        # Business logic use cases
├── notification/       # AlarmManager, BroadcastReceivers
├── ui/
│   ├── today/          # Today screen + onboarding
│   ├── calendar/       # 17-day grid + cycle navigation
│   ├── stats/          # Analytics + progress ring
│   ├── settings/       # Reminder config + permissions
│   └── theme/          # Colors, typography
└── di/                 # Hilt modules
```

## Setup

### Requirements

- Android Studio Hedgehog or newer
- Android device or emulator with API 26+
- Training videos placed in the device's video storage

### Video Files

The app expects 17 video files accessible via MediaStore (Videos). Place them in the device's video storage (e.g. `Movies/` or `Download/`). The app resolves files by filename with fuzzy matching (case-insensitive, Polish character normalization).

### Build & Run

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on emulator or device

```bash
git clone https://github.com/michalbandyszak/mobilne-cialo.git
cd mobilne-cialo
# open in Android Studio or:
./gradlew assembleDebug
```

### First Launch

On first launch the app asks for a program start date. Choose today to start from day 1, or pick a past date if you've already been training. Weekday schedules can be changed in Settings.

## How the 17-Day Cycle Works

```
totalDays    = days since startDate
currentCycle = (totalDays / 17) + 1
currentVideo = (totalDays % 17) + 1
```

The mobility cycle repeats indefinitely and advances only on selected mobility weekdays. Strength training rotates between Day 1 and Day 2 on selected strength weekdays.

## Permissions

| Permission | Why |
|-----------|-----|
| `READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE` | Access training videos |
| `POST_NOTIFICATIONS` | Daily reminder notifications |
| `SCHEDULE_EXACT_ALARM` | Precise reminder timing |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Reliable alarm delivery |

## License

MIT
