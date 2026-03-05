# Sleep Saver - Context-Aware Sleep Companion

**Developer:** Jacob Murphy (worked alone)

Sleep Saver infers bedtime automatically using on-device context signals and logs nightly sleep behavior without requiring a manual "Start Sleep" button.

## Core Concept

The app intelligently detects sleep using **sensor fusion** and environmental signals:

1. **Light Sensor** (Hardware #1) - Detects dark environment (lights-off behavior)
2. **Proximity Sensor** (Hardware #2) - Detects face-down phone placement (intentional sleep position)
3. **Battery Charging State** (Virtual Sensor) - Indicates nighttime routine and device availability
4. **Time Window** - Restricts detection to typical sleep hours (10 PM - 2 AM, configurable)

## How It Works

### Sleep Detection (Inference Logic)
Sleep Mode activates **only when ALL conditions are met:**
- Ambient light < threshold (default: 8 lux)
- Phone is face-down (proximity sensor near)
- Device is actively charging
- Current time is within configured sleep window

This multi-signal fusion prevents false positives from accidental dim lighting or casual charging.

### Disturbance Tracking
During active sleep mode (between 12 AM and 6 AM), the app detects disturbances:
- **Accelerometer motion spikes** - Sudden movement exceeding threshold (6 m/s² change)
- **Screen unlock events** - User interaction indicating potential wake

Each disturbance is logged to the database for analysis.

## Architecture: MVVM Pattern

### Model Layer (`data/` package)
- **Room Database** (`SleepSaverDatabase`, `SleepSessionEntity`, `DisturbanceEventEntity`)
  - Persists sleep sessions with start/end times
  - Stores individual disturbance events linked to sessions
  - Accessed via `SleepDao` and `SleepRepository`
- **DataStore Preferences** (`SettingsRepository`, `ContextSnapshotStore`)
  - User settings: detection enabled, light threshold, sleep window, auto-DND toggle, dark mode
  - Latest sensor snapshot for quick access

### View Layer (`ui/` package)
- **Compose UI** in `MainActivity.kt`
  - **Dashboard**: Sleep status, tonight's start time, disturbance count, weekly summary
  - **Context Logic**: Explains sensor fusion and detection logic
  - **Privacy by Design**: Data handling explanation and history deletion
  - **Settings**: User toggles and preferences
- **Theme** (`ui/theme/`)
  - Custom Indigo/Lavender color palette
  - Rounded card shapes (12dp, 18dp, 24dp)
  - Light and dark mode support

### ViewModel Layer (`SleepViewModel.kt`)
- Coordinates all app logic:
  - Observes sensor flows from `EnvironmentSensorMonitor`
  - Applies sleep inference via `SleepInferenceEngine`
  - Manages database writes to `SleepRepository`
  - Combines all streams into reactive `DashboardUiState`
- Handles user interactions (settings changes, history deletion)
- Manages sleep brightness control

## Background Task

**WorkManager Integration** (`SleepCheckWorker`, `WorkScheduler`):
- Periodic task runs every 15 minutes
- Runs even if app is closed or device locked
- Re-evaluates sleep inference conditions
- Automatically starts sleep session if conditions are met
- Gracefully handles app lifecycle

## Features

### Dashboard Screen
- Real-time status: Awake / Sleep Mode Active
- Tonight's sleep start time (formatted HH:MM AM/PM)
- Current disturbance count
- Consistency score (based on sleep start time variance)
- Weekly summary: Start times, durations, disturbance counts
- Live sensor snapshot: ambient light (lux), charging state, face-down status
- Adaptive layout: Single column on phones, two-pane on tablets (≥700dp)

### Context Logic Screen
Explains:
- Why ambient light matters (lights-off behavior indicator)
- Why proximity matters (face-down detection, not facial recognition)
- Why charging matters (bedtime routine signal)
- Time window role (prevents false positives outside typical sleep hours)
- How all signals combine for robust inference

### Privacy by Design Screen
- Certifies light sensor doesn't record images (lux values only)
- Confirms proximity sensor doesn't identify people (near/far only)
- Affirms no audio recording, no location tracking
- Documents local-only storage (Room database + DataStore)
- Provides history deletion switch (clears all sessions and disturbances)

### Settings Screen
User-configurable toggles and sliders:
- **Sleep Detection**: Enable/disable the entire inference engine
- **Light Threshold Slider**: Adjust sensitivity (1-50 lux)
- **Sleep Window Times**: Start hour and end hour (0-23)
- **Auto Do Not Disturb**: Toggle automatic DND when sleep detected
- **Manual Dark Mode**: Toggle dark theme (independent of system preferences)

## Permissions

- `BODY_SENSORS` - Light and proximity sensor access
- `ACCESS_NOTIFICATION_POLICY` - Do Not Disturb control
- `SCHEDULE_EXACT_ALARM` - WorkManager background task scheduling
- `INTERNET` - Network connectivity for background work

## Data Persistence

- **Room Database** (`sleep_saver.db`)
  - 2 tables: `sleep_sessions` and `disturbance_events`
  - Auto-increment IDs, foreign key relationships
  - Version: 1 (no migrations yet)
- **DataStore Preferences**
  - User settings (detection, thresholds, times, toggles)
  - Latest sensor snapshot for offline inference

## Build and Test

### Build APK
```powershell
cd "C:\Users\jjmur\Documents\Programming\CS-4518\Sleep-Saver"
.\gradlew.bat :app:assembleDebug --console=plain
```

### Run Unit Tests
```powershell
.\gradlew.bat :app:testDebugUnitTest --console=plain
```

### Run on Emulator
```powershell
.\gradlew.bat :app:installDebug --console=plain
adb shell am start -n com.example.sleepsaver/.MainActivity
```

## Quick Test Flow (Professor Instructions)

### Test Sleep Detection

1. **Launch app** on emulator or device
2. **Set system time to 11:00 PM** (within default sleep window 10 PM - 2 AM)
   - Emulator: Settings > System > Date & time > Set time
3. **Simulate charging**: Emulator > Battery icon > Set charging state to "Charging"
4. **Simulate dark environment**: 
   - Emulator: Extended controls > Sensors > Light > Set to 5 lux (below 8 lux threshold)
5. **Simulate face-down placement**: 
   - Emulator: Extended controls > Sensors > Proximity > Set to 0 cm (near = face down)
6. **Wait for detection** (up to 1 minute for main app, up to 15 minutes for WorkManager background check)
7. **Verify Dashboard**:
   - Status should change to "Sleep Mode Active"
   - Tonight's sleep start time should appear
   - Sensor snapshot should show: Charging, Face Down, Low light

### Test Disturbance Logging

1. **Ensure sleep is active** (follow sleep detection steps above)
2. **Verify time is between 12:00 AM and 6:00 AM**
   - Change system time to 1:00 AM if needed
3. **Trigger accelerometer spike**:
   - Emulator: Extended controls > Sensors > Accelerometer > Shake device (increase magnitude changes)
4. **Alternative: Trigger screen unlock**
   - Emulator: Press power button or press unlock
5. **Refresh app** or wait for ticker update (max 60 seconds)
6. **Verify Dashboard**:
   - Disturbance count should increment
   - Weekly summary should reflect disturbance

### Test Dark Mode Toggle

1. Navigate to **Settings** screen
2. Find "Manual dark mode" toggle
3. Toggle on/off
4. Verify UI colors switch between:
   - **Light mode**: Soft morning tones (light background, indigo accents)
   - **Dark mode**: Deep blue/indigo palette (dark background, lavender accents)

### Test Settings Persistence

1. Adjust settings (light threshold, sleep window, etc.)
2. Close app completely
3. Relaunch app
4. Verify all settings are restored

## Architecture Compliance

### MVVM Separation
- **Model**: All data logic in `data/local/` (Room, DAOs) and `data/settings/` (DataStore)
- **View**: All UI rendering in `MainActivity.kt` (Compose screens)
- **ViewModel**: All app logic in `SleepViewModel.kt` (sensor fusion, inference, coordination)

### Adaptive Layout
- Detects `LocalConfiguration.screenWidthDp`
- Phones (<700dp): Vertical stacked layout
- Tablets (≥700dp): Two-pane layout (status left, weekly summary right)

### Custom Theme
- `ui/theme/Color.kt`: Indigo Primary (0xFF2E3A8C), Lavender Accent (0xFFB39DDB), custom night/morning colors
- `ui/theme/Shape.kt`: Rounded corners (12/18/24 dp)
- `ui/theme/Theme.kt`: Light and dark color schemes applied via `SleepSaverTheme()`

### Navigation
- Bottom navigation bar with 4 destinations
- Navigation state persisted with `saveState = true` and `restoreState = true`

## References & Resources

- **Android Sensors API**: https://developer.android.com/guide/topics/sensors
- **Room Database**: https://developer.android.com/training/data-storage/room
- **DataStore Preferences**: https://developer.android.com/topic/libraries/architecture/datastore
- **WorkManager**: https://developer.android.com/topic/libraries/architecture/workmanager
- **Compose Layout**: https://developer.android.com/jetpack/compose/layouts
- **Material 3 Theme**: https://developer.android.com/develop/ui/compose/designsystems/material3

## Notes

- DND auto-toggle requires device-level permission grant at runtime (API 31+)
- `android.disallowKotlinSourceSets=false` in `gradle.properties` allows KSP with current AGP
- Disturbance detection respects sleep mode: only logs between 12 AM - 6 AM during active sleep
- Light threshold default (8 lux) is calibrated for typical indoor lighting transition
- Accelerometer threshold (6 m/s² change) filters out environmental vibrations


