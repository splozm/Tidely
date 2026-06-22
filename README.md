# Tidely

UK tide times on your Android home screen.

## Overview

Tidely is an Android app that displays UK tide predictions directly on your home screen via a widget. No need to open an app – the next high and low tide times are always visible at a glance.

## Features

- **Home Screen Widget** – Shows next high/low tide times and heights
- **607 UK Stations** – Full coverage of UK coastal tidal stations
- **Offline Support** – Works offline once data is fetched
- **Auto Location** – Automatically finds your nearest tidal station via GPS
- **Manual Selection** – Browse and select any of 607 UK stations
- **Smart Updates** – Widget updates only when tide events change (2× per day)

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 34 (Android 14)
- **Architecture:** MVVM
- **Database:** Room
- **Networking:** Retrofit + OkHttp
- **Location:** Google Play Services Location
- **Charts:** MPAndroidChart
- **Background Work:** WorkManager

## Project Structure

```
app/
├── data/
│   ├── api/          # Admiralty API client
│   ├── db/           # Room database, DAOs
│   ├── model/        # Data models (Station, TidalEvent)
│   └── repository/   # TideRepository
├── ui/
│   ├── main/         # Main app activity + tide view
│   └── settings/     # Settings & station selection
├── widget/
│   └── TideWidgetProvider.kt
└── util/
    └── LocationHelper.kt
```

## Setup

### 1. Clone the repository

```bash
git clone <repo-url>
cd Tidely
```

### 2. Get API Key

1. Sign up at https://admiraltyapi.portal.azure-api.net/
2. Subscribe to the **UK Tidal API - Discovery** (free tier)
3. Copy your API subscription key

### 3. Configure API Key

Create a `local.properties` file in the project root:

```properties
ADMIRALTY_API_KEY=your_api_key_here
```

⚠️ **Never commit `local.properties` to version control**

### 4. Build & Run

Open the project in Android Studio and build.

Or via command line:

```bash
./gradlew assembleDebug
```

## Data Source

Tidal predictions are sourced from the **ADMIRALTY UK Tidal API**, operated by the UK Hydrographic Office. The API provides:

- 607 tidal stations across the UK
- Current + 6 days of tidal event predictions
- Heights in metres
- Times in UK local time (GMT/BST)

Discovery tier is **free for 1 year**.

## Widget Update Strategy

The widget uses smart scheduling to minimize battery drain:

- Fetches **7 days** of tide data at once
- Widget updates **only when displayed tide events change** (~2× per day)
- Uses WorkManager to schedule the next update right after a tide event passes
- No polling or wasteful updates

## Roadmap

### v1.0 (Current)
- ✅ Android app
- ✅ Home screen widget (2×1 and 4×2)
- ✅ GPS auto-location
- ✅ Manual station selection
- ✅ Offline support

### v2.0 (Future)
- iOS version
- Apple Watch complication
- Tide alerts/notifications
- Swell and wind data
- Multiple saved locations

## License

TBD

## Privacy

- Location data is used only to find the nearest tidal station
- No tracking or analytics beyond Firebase Crashlytics
- No ads
- No personal data collected

## Contributing

TBD
