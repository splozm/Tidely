# Tidely – Technical Specification

## Platform & Language

- **Platform:** Android native
- **Language:** Kotlin
- **Minimum SDK:** API 26 (Android 8.0 Oreo)
- **Target SDK:** API 34 (Android 14)
- **Build tool:** Gradle
- **IDE:** Android Studio

---

## Architecture

MVVM (Model-View-ViewModel) pattern.

```
app/
├── data/
│   ├── api/          # Admiralty API client
│   ├── model/        # Data models (Station, TidalEvent)
│   └── repository/   # TideRepository (fetches + caches)
├── ui/
│   ├── main/         # Main app activity + tide table view
│   └── settings/     # Station selection
├── widget/
│   └── TideWidget.kt # Home screen widget provider
└── util/
    └── LocationHelper.kt
```

---

## Data Layer

### API
- **Provider:** ADMIRALTY UK Tidal API
- **Base URL:** `https://admiraltyapi.azure-api.net/uktidalapi/api/V1/`
- **Auth:** Ocp-Apim-Subscription-Key header
- **Client:** Retrofit + OkHttp

### Key Endpoints

**Get all stations**
```
GET /Stations
```
Returns GeoJSON FeatureCollection of all 607 UK tidal stations with coordinates.

**Get tidal events**
```
GET /Stations/{stationId}/TidalEvents?duration=6
```
Returns high/low tide events for next 6 days including time and height.

### Caching
- Station list cached locally on first launch (rarely changes)
- Tidal events cached for current day, refreshed at midnight or on manual refresh
- Cache stored in Room database
- Widget reads from cache — no live API call on widget render

---

## Location

- Uses Android `FusedLocationProviderClient` for GPS
- On first launch, requests location permission
- Calculates nearest station using Haversine distance formula against cached station list
- Falls back to last known location if GPS unavailable

---

## Home Screen Widget

- Implemented as `AppWidgetProvider` (Kotlin)
- Layout via `RemoteViews` (required for Android widgets)
- Displays:
  - Station name
  - Next high tide: time + height (m)
  - Next low tide: time + height (m)
  - Rising / Falling indicator
- Update interval: every 30 minutes via `AlarmManager`
- Widget sizes supported: 2x1, 4x1, 4x2

---

## Permissions

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED"/>
```

RECEIVE_BOOT_COMPLETED required to restart widget update scheduler after device reboot.

---

## Dependencies

```kotlin
// Networking
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:okhttp:4.12.0")

// Local database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")

// Location
implementation("com.google.android.gms:play-services-location:21.0.1")

// UI
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("com.github.PhilJay:MPAndroidChart:3.1.0") // tide graph
```

---

## API Key Handling

- API key stored in `local.properties` (never committed to source control)
- Injected at build time via `BuildConfig`
- Not hardcoded anywhere in source

```
// local.properties
ADMIRALTY_API_KEY=your_key_here
```

```kotlin
// build.gradle.kts
buildConfigField("String", "ADMIRALTY_API_KEY", "\"${properties["ADMIRALTY_API_KEY"]}\"")
```

---

## Google Play

- **Price:** £0.99
- **Category:** Weather
- **Content rating:** Everyone
- **Developer account:** £20 one-off registration fee
- Target markets: United Kingdom

---

## Build & Release

1. Clone repo
2. Add API key to `local.properties`
3. `./gradlew assembleRelease`
4. Sign APK with release keystore
5. Upload to Google Play Console
